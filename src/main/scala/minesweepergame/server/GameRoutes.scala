package minesweepergame.server

import cats.effect.{IO, Ref}
import minesweepergame.game.{GameLevel, GameSession}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._

import java.util.UUID

object GameRoutes {
  def apply(games: Ref[IO, Map[UUID, GameSession]]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case POST -> Root / "game" / "create" / level =>
      GameLevel.gameLevel(level).fold(BadRequest("Invalid level")) { gameLevel =>
        for {
          newGameSession <- GameSession.create("Player", System.currentTimeMillis(), None, gameLevel)
          gameId = UUID.randomUUID()
          _ <- games.update(_.updated(gameId, newGameSession))
          response <- Ok(s"Game created with the ID: $gameId")
        } yield response
      }
  }
}
