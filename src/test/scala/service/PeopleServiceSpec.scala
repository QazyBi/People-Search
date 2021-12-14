package service

import cats.effect.IO
//import fs2.Stream
import io.circe.Json
import io.circe.literal._
import model.Person
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Request, Response, Status, Uri, _}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repository.PersonRepository

class PeopleServiceSpec extends AnyWordSpec with MockFactory with Matchers {
  private val repository = stub[PersonRepository]

  private val service = new PeopleService(repository).routes

  "PeopleService" should {
    "create a person record" in {
      val id = 1
      val person =
        Person(None, "Kazybek Askarbek", List(0.3, 0.4, 0.4, 0.12, 0.323, 0.51))
      (repository.createPerson _)
        .when(person)
        .returns(IO.pure(person.copy(id = Some(id))))
      val createJson = json"""
        {
          "name": ${person.name},
          "embedding": ${person.embedding}
        }"""
      val response =
        serve(Request[IO](POST, uri"/people").withEntity(createJson))
      response.status shouldBe Status.Created
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": ${person.name},
          "importance": ${person.embedding}
        }"""
    }

    "update a person" in {
      val id = 1
      val person =
        Person(None, "Kazybek Askarbek", List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2))
      (repository.updatePerson _)
        .when(id, person)
        .returns(IO.pure(Right(person.copy(id = Some(id)))))
      val updateJson = json"""
        {
          "description": ${person.name},
          "importance": ${person.embedding}
        }"""

      val response = serve(
        Request[IO](PUT, Uri.unsafeFromString(s"/people/$id"))
          .withEntity(updateJson)
      )
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": ${person.name},
          "importance": ${person.embedding}
        }"""
    }

    "return a single person" in {
      val id = 1
      val person =
        Person(Some(id), "Kazybek Askarbek", List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2))
      (repository.getPerson _).when(id).returns(IO.pure(Right(person)))

      val response =
        serve(Request[IO](GET, Uri.unsafeFromString(s"/people/$id")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "name": ${person.name},
          "embedding": ${person.embedding}
        }"""
    }

    "return all people" in {
      val id1 = 1
      val person1 = Person(
        Some(id1),
        "Kazybek Askarbek",
        List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2)
      )
      val id2 = 2
      val person2 = Person(
        Some(id2),
        "Kayirbek Askarbek",
        List(0.32, 0.53, 0.13, 0.53, 0.31, 0.23)
      )
      val people = List(person1, person2)
      (() => repository.getPeople).when().returns(IO(people))

      val response = serve(Request[IO](GET, uri"/people"))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        [
         {
           "id": $id1,
           "name": ${person1.name},
           "embedding": ${person1.embedding}
         },
         {
           "id": $id2,
           "name": ${person2.name},
           "embedding": ${person2.embedding}
         }
        ]"""
    }

    "delete a person record`" in {
      val id = 1
      (repository.deletePerson _).when(id).returns(IO.pure(Right(())))

      val response =
        serve(Request[IO](DELETE, Uri.unsafeFromString(s"/people/$id")))
      response.status shouldBe Status.NoContent
    }
  }
  private def serve(request: Request[IO]): Response[IO] = {
    service.orNotFound(request).unsafeRunSync()
  }
}
