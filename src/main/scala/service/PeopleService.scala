package service
import cats.effect.IO
//import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import model.{Person, PersonNotFoundError}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.{HttpRoutes, MediaType, Uri}
import repository.PersonRepository

class PeopleService(repository: PersonRepository) extends Http4sDsl[IO] {
  val routes = HttpRoutes.of[IO] {
    case GET -> Root / "people" => {
      for {
        p <- repository.getPeople
        response <- Ok(p)
      } yield response
    }

    case GET -> Root / "people" / LongVar(id) => {
      for {
        person <- repository.getPerson(id)
        response <- peopleResult(person)
      } yield response
    }
    case GET -> Root / "search" / LongVar(id) => {
      val result = for {
        person <- repository.getPerson(id)
        people <- repository.getPeople
        similarity = person match {
          case Right(p) =>
            Ok(
              people
                .map(p2 =>
                  (
                    p2,
                    CosineSimilarity.cosineSimilarity(p.embedding, p2.embedding)
                  )
                )
                .sortWith((p1, p2) => p1._2 > p2._2)
                .slice(1, 6) // select 5 most similar people
                .map(_._1)
            )
          case _ => NotFound()
        }
      } yield similarity
      result.unsafeRunSync()
    }

    case req @ POST -> Root / "people" =>
      val res = for {
        person <- req.decodeJson[Person]
        createdPerson <- repository.createPerson(person)
//        response <- Created(
//          createdPerson.asJson,
//          Location(Uri.unsafeFromString(s"/people/${createdPerson.id.get}"))
//        )
      } yield createdPerson
      Ok(res)
    case req @ PUT -> Root / "people" / LongVar(id) =>
      for {
        person <- req.decodeJson[Person]
        updateResult <- repository.updatePerson(id, person)
        response <- peopleResult(updateResult)
      } yield response

    case DELETE -> Root / "people" / LongVar(id) =>
      repository.deletePerson(id).flatMap {
        case Left(PersonNotFoundError) => NotFound()
        case Right(_)                  => NoContent()
      }
  }

  private def peopleResult(result: Either[PersonNotFoundError.type, Person]) = {
    result match {
      case Left(PersonNotFoundError) => NotFound()
      case Right(person)             => Ok(person)
    }
  }
}
