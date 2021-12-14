import cats.effect.{ContextShift, IO, Timer}
import config.Config
import io.circe.Json
import io.circe.literal._
import io.circe.optics.JsonPath._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import repository.PersonRepository
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class PeopleServerSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with Eventually {
  private implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  private implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  private lazy val client = BlazeClientBuilder[IO](global).resource

  private val configFile = "test.conf"

  private lazy val config =
    Config.load(configFile).use(config => IO.pure(config)).unsafeRunSync()

  private lazy val urlStart =
    s"http://${config.server.host}:${config.server.port}"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(5, Seconds)),
    interval = scaled(Span(100, Millis))
  )

  override def beforeAll(): Unit = {
    HttpServer.create(configFile).unsafeRunAsyncAndForget()
    eventually {
      client
        .use(_.statusFromUri(Uri.unsafeFromString(s"$urlStart/people")))
        .unsafeRunSync() shouldBe Status.Ok
    }
    ()
  }

  "People server" should {
    "create a person record" in {
      val name = "Kazybek Askarbek"
      val embedding = List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2)
      val createJson = json"""
        {
          "name": $name,
          "embedding": $embedding
        }"""
      val request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(s"$urlStart/people")
      ).withEntity(createJson)
      val json = client.use(_.expect[Json](request)).unsafeRunSync()
      root.id.long.getOption(json).nonEmpty shouldBe true
      root.description.string.getOption(json) shouldBe Some(name)
      root.importance.string.getOption(json) shouldBe Some(embedding)
    }

    "update a person record" in {
      val id =
        createPerson("Kazybek Askarbek", List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2))

      val name = "Kayirbek Askarbek"
      val embedding = List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2)
      val updateJson = json"""
        {
          "name": $name,
          "embedding": $embedding
        }"""
      val request = Request[IO](
        method = Method.PUT,
        uri = Uri.unsafeFromString(s"$urlStart/people/$id")
      ).withEntity(updateJson)
      client.use(_.expect[Json](request)).unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "name": $name,
          "embedding": $embedding
        }"""
    }

    "return a single person" in {
      val name = "Kazybek Askarbek"
      val embedding = List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2)
      val id = createPerson(name, embedding)
      client
        .use(_.expect[Json](Uri.unsafeFromString(s"$urlStart/people/$id")))
        .unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "name": $name,
          "embedding": $embedding
        }"""
    }

    "delete a person record" in {
      val name = "Kazybek Askarbek"
      val embedding = List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2)
      val id = createPerson(name, embedding)
      val deleteRequest = Request[IO](
        method = Method.DELETE,
        uri = Uri.unsafeFromString(s"$urlStart/people/$id")
      )
      client
        .use(_.status(deleteRequest))
        .unsafeRunSync() shouldBe Status.NoContent

      val getRequest = Request[IO](
        method = Method.GET,
        uri = Uri.unsafeFromString(s"$urlStart/people/$id")
      )
      client.use(_.status(getRequest)).unsafeRunSync() shouldBe Status.NotFound
    }

    "return all people" in {
      // Remove all existing people
      val json = client
        .use(_.expect[Json](Uri.unsafeFromString(s"$urlStart/people]")))
        .unsafeRunSync()
      root.each.id.long.getAll(json).foreach { id =>
        val deleteRequest = Request[IO](
          method = Method.DELETE,
          uri = Uri.unsafeFromString(s"$urlStart/people/$id")
        )
        client
          .use(_.status(deleteRequest))
          .unsafeRunSync() shouldBe Status.NoContent
      }

      // Add new people
      val name1 = "Kazybek Askarbek"
      val name2 = "Kayirbek Askarbek"
      val embedding1 = List(0.3, 0.5, 0.1, 0.5, 0.1, 0.2)
      val embedding2 = List(0.33, 0.53, 0.13, 0.53, 0.13, 0.23)
      val id1 = createPerson(name1, embedding1)
      val id2 = createPerson(name2, embedding2)

      // Retrieve people
      client
        .use(_.expect[Json](Uri.unsafeFromString(s"$urlStart/people")))
        .unsafeRunSync() shouldBe json"""
        [
          {
            "id": $id1,
            "name": $name1,
            "embedding": $embedding1
          },
          {
            "id": $id2,
            "name": $name2,
            "embedding": $embedding2
          }
        ]"""
    }
  }

  private def createPerson(name: String, embedding: List[Double]): Long = {
    val createJson = json"""
      {
        "name": $name,
        "embedding": $embedding
      }"""
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(s"$urlStart/people")
    ).withEntity(createJson)
    val json = client.use(_.expect[Json](request)).unsafeRunSync()
    root.id.long.getOption(json).nonEmpty shouldBe true
    root.id.long.getOption(json).get
  }
}
