
scalaVersion := "2.13.13"
version := "0.1.0-SNAPSHOT"

val http4sVersion = "1.0.0-M40"
val circeVersion = "0.15.0-M1"

lazy val root = (project in file("."))
  .settings(
    name := "minesweeper",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test
    )
  )