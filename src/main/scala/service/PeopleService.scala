package service
import scala.language.postfixOps
import cats.effect.IO
import fs2.Stream
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
//  private implicit val encodeImportance: Encoder[Person] =
//    Encoder.encodeString.contramap[Person(_.value)

//  private implicit val decodeImportance: Decoder[Importance] =
//    Decoder.decodeString.map[Person](Importance.unsafeFromString)

  val routes = HttpRoutes.of[IO] {
//    case GET -> Root / "people" =>
//      Ok(
//        Stream("[") ++ repository.getPeople
//          .map(_.asJson.noSpaces)
//          .intersperse(",") ++ Stream("]"),
//        `Content-Type`(MediaType.application.json)
//      )

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
                .slice(1, 3)
                .map(_._1.name)
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
      } yield person // response
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

  object CosineSimilarity {
    // inspired by: https://gist.github.com/reuben-sutton/2932974
    def cosineSimilarity(emb1: List[Double], emb2: List[Double]): Double = {
      dotProduct(emb1, emb2) / (magnitude(emb1) * magnitude(emb2))
    }
    def magnitude(x: List[Double]): Double = {
      math.sqrt(x map (i => i * i) sum)
    }

    def dotProduct(x: List[Double], y: List[Double]): Double = {
      (for ((a, b) <- x zip y) yield a * b) sum
    }
  }

  private def peopleResult(result: Either[PersonNotFoundError.type, Person]) = {
    result match {
      case Left(PersonNotFoundError) => NotFound()
      case Right(person)             => Ok(person)
    }
  }
}
