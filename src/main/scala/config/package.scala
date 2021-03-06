import cats.effect.{Blocker, ContextShift, IO, Resource}
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

package object config {
  case class ServerConfig(host: String, port: Int)

  case class DatabaseConfig(
      driver: String,
      url: String,
      user: String,
      password: String,
      threadPoolSize: Int
  )
  // compound class containing both configuration files
  case class Config(server: ServerConfig, database: DatabaseConfig)

  object Config {
    // function to load configuration file into Config class
    def load(
        configFile: String = "application.conf"
    )(implicit cs: ContextShift[IO]): Resource[IO, Config] = {
      Blocker[IO].flatMap { blocker =>
        Resource.eval(
          ConfigSource
            .fromConfig(ConfigFactory.load(configFile))
            .loadF[IO, Config](blocker)
        )
      }
    }
  }
}
