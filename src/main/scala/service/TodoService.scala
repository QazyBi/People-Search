package service

import cats.effect.IO
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import model.{
  Person,
  PersonNotFoundError
} //{Importance, Todo, TodoNotFoundError}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.{HttpRoutes, MediaType, Uri}
import repository.PersonRepository

class TodoService(repository: PersonRepository) extends Http4sDsl[IO] {
//  private implicit val encodeImportance: Encoder[Person] =
//    Encoder.encodeString.contramap[Person(_.value)
//
//  private implicit val decodeImportance: Decoder[Importance] =
//    Decoder.decodeString.map[Person](Importance.unsafeFromString)

  val routes = HttpRoutes.of[IO] {
    case GET -> Root / "todos" =>
      Ok(
        Stream("[") ++ repository.getPeople
          .map(_.asJson.noSpaces)
          .intersperse(",") ++ Stream("]"),
        `Content-Type`(MediaType.application.json)
      )

    case GET -> Root / "todos" / LongVar(id) =>
      for {
        getResult <- repository.getPerson(id)
        response <- todoResult(getResult)
      } yield response

    case req @ POST -> Root / "todos" =>
      for {
        todo <- req.decodeJson[Person]
        createdTodo <- repository.createPerson(todo)
        response <- Created(
          createdTodo.asJson,
          Location(Uri.unsafeFromString(s"/todos/${createdTodo.id.get}"))
        )
      } yield response

    case req @ PUT -> Root / "todos" / LongVar(id) =>
      for {
        todo <- req.decodeJson[Person]
        updateResult <- repository.updatePerson(id, todo)
        response <- todoResult(updateResult)
      } yield response

    case DELETE -> Root / "todos" / LongVar(id) =>
      repository.deletePerson(id).flatMap {
        case Left(PersonNotFoundError) => NotFound()
        case Right(_)                  => NoContent()
      }
  }

  private def todoResult(result: Either[PersonNotFoundError.type, Person]) = {
    result match {
      case Left(PersonNotFoundError) => NotFound()
      case Right(todo)               => Ok(todo.asJson)
    }
  }
}
