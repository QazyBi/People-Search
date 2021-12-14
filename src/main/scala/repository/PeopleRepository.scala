package repository

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import model.{Person, PersonNotFoundError}
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._

class PersonRepository(transactor: Transactor[IO]) {
  def getPeople: IO[List[Person]] = { // Stream[IO, Person]
    sql"SELECT id, name, embedding FROM people"
      .query[Person]
//      .stream
      .to[List]
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
    sql"UPDATE people SET name = ${person.name}, embedding = ${person.embedding} WHERE id = $id".update.run
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
