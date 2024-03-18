package minesweepergame.server

import cats.effect.{IO, Ref}
import minesweepergame.game.{GameId, GameLevel, GameSession}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import io.circe.syntax._

import java.util.UUID

object GameRoutes {
  def apply(games: Ref[IO, Map[UUID, GameSession]]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case POST -> Root / "game" / "create" / GameLevel(level) =>
      GameLevel.unapply(level.toString).fold(BadRequest("Invalid level")) { gameLevel =>
        for {
          instant <- IO.realTimeInstant
          newGameSession <- GameSession.create("Player", instant, None, gameLevel) // creates a new GameSession
          gameId <- IO.randomUUID // creates a new UUID
          _ <- games.update(_.updated(gameId, newGameSession))
          response <- Ok(s"Game created with the ID: $gameId")
        } yield response // responds with UUID
      }

    case req @ POST -> Root / "game" / "command" / GameId(gameId) =>
      for {
        command <- req.as[Command]
        now <- IO.realTimeInstant
        updatedGameSession <- games.modify { gameSessions =>
          gameSessions.get(gameId) match {
            case Some(gameSession) =>
              val updatedSession = gameSession.handleCommand(command, now)
              (gameSessions.updated(gameId, updatedSession), Some(updatedSession))
            case None => (gameSessions, None)
          }
        }
        response <- updatedGameSession match {
          case Some(session) => Ok(session.asJson)
          case None => NotFound("Game session not found")
        }
      } yield response
  }
}
