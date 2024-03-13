package minesweepergame.server

import cats.effect.{IO, Ref}
import minesweepergame.game.GameSession
import org.http4s.HttpRoutes
import org.http4s.dsl.io._

import java.util.UUID

object GameRoutes {
  def apply(games: Ref[IO, Map[UUID, GameSession]]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case POST -> Root / "game" / "create" =>
      ??? // game creation logic here
  }
}
