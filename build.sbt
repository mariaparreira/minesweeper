
scalaVersion     := "2.13.13"
version          := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "minesweeper",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test
    )
  )