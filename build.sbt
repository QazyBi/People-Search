lazy val commonSettings = Seq(
  name := "people-search",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.12.11",
  scalacOptions ++= Seq(
    "-deprecation",
    "-Xfatal-warnings",
    "-Ywarn-value-discard",
    "-Xlint:missing-interpolator"
  )
)

lazy val Http4sVersion = "0.21.15"

lazy val DoobieVersion = "0.13.4"

lazy val H2Version = "1.4.200"

lazy val FlywayVersion = "7.5.2"

lazy val CirceVersion = "0.13.0"

lazy val PureConfigVersion = "0.12.3"

lazy val LogbackVersion = "1.2.3"

lazy val ScalaTestVersion = "3.2.3"

lazy val ScalaMockVersion = "5.1.0"

scalacOptions += "-feature"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion % "it,test",
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      //      "org.tpolecat"          %% "doobie-h2"            % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
//      "org.tpolecat" %% "doobie-contrib-postgresql" % "0.3.0",
      "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
      //      "com.h2database"        %  "h2"                   % H2Version,
      "org.flywaydb" % "flyway-core" % FlywayVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion % "it,test",
      "io.circe" %% "circe-optics" % CirceVersion % "it",
      "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "it,test",
      "org.scalamock" %% "scalamock" % ScalaMockVersion % "test"
//      "com.github.sbt" % "sbt-native-packager" % "1.9.7"
//      "com.github.sbt" % "sbt-native-packager_2.12_1.0" % "1.9.7"
    )
  )
