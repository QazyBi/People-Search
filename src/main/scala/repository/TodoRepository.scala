package repository

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import model.{
  Person,
  PersonNotFoundError
} /// {Importance, Todo, TodoNotFoundError}
import doobie._
import doobie.implicits._

class PersonRepository(transactor: Transactor[IO]) {
//  private implicit val importanceMeta: Meta[Importance] =
//    Meta[String].timap(Importance.unsafeFromString)(_.value)

  def getPeople: Stream[IO, Person] = {
    sql"SELECT id, name, embedding FROM people"
      .query[Person]
      .stream
      .transact(transactor)
  }

  def getPerson(id: Long): IO[Either[PersonNotFoundError.type, Person]] = {
    sql"SELECT id, name, embedding FROM people WHERE id = $id"
      .query[Person]
      .option
      .transact(transactor)
      .map {
        case Some(person) => Right(person)
        case None         => Left(PersonNotFoundError)
      }
  }

  def createPerson(person: Person): IO[Person] = {
    sql"INSERT INTO people (name, embedding) VALUES (${person.name}, ${person.embedding})".update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(transactor)
      .map { id =>
        person.copy(id = Some(id))
      }
  }

  def deletePerson(id: Long): IO[Either[PersonNotFoundError.type, Unit]] = {
    sql"DELETE FROM people WHERE id = $id".update.run.transact(transactor).map {
      affectedRows =>
        if (affectedRows == 1) {
          Right(())
        } else {
          Left(PersonNotFoundError)
        }
    }
  }

  def updatePerson(
      id: Long,
      person: Person
  ): IO[Either[PersonNotFoundError.type, Person]] = {
    sql"UPDATE people SET class = ${person.name}, embedding = ${person.embedding} WHERE id = $id".update.run
      .transact(transactor)
      .map { affectedRows =>
        if (affectedRows == 1) {
          Right(person.copy(id = Some(id)))
        } else {
          Left(PersonNotFoundError)
        }
      }
  }
}
