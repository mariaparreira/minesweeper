
scalaVersion     := "2.12.8"
version          := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "minesweeper",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.2.9",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "co.fs2" %% "fs2-core" % "3.1.4",
      "co.fs2" %% "fs2-io" % "3.1.4"
    )
  )