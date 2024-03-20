package minesweepergame.server

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._

object HealthRoutes {
  def apply(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok("I am up and running.")
  }
}

// Defines a simple http route for checking the health of the server.
// When a GET request is made to the /health endpoint, it responds with a message indicating that the server is running.