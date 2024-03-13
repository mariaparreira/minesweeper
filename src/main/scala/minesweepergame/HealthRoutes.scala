package minesweepergame

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._

object HealthRoutes {
  def apply(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok("I am up and running.")
  }
}
