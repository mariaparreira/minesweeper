package minesweepergame.server

import cats.effect.{IO, Ref}
import minesweepergame.game.{GameLevel, GameSession, Uuid}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import io.circe.syntax._
import org.http4s.server.AuthMiddleware

import java.util.UUID

object GameRoutes {
  def apply(games: Ref[IO, Map[UUID, GameSession]], authMiddleware: AuthMiddleware[IO, UUID]): HttpRoutes[IO] = {
        val authedRoutes: AuthedRoutes[UUID, IO] = AuthedRoutes.of[UUID, IO] {
          case POST -> Root / "game" / "create" / GameLevel(level) as playerId =>
            for {
              instant <- IO.realTimeInstant
              newGameSession <- GameSession.create(playerId, "Player", instant, None, level) // creates a new GameSession and passes the playerId
              gameId <- IO.randomUUID // creates a new UUID for gameId
              _ <- games.update(_.updated(gameId, newGameSession))
              response <- Ok(s"Game created with the ID: $gameId")
            } yield response // responds with UUID

          case req@POST -> Root / "game" / "command" / Uuid(id) as playerId =>
            for {
              command <- req.req.as[Command]
              now <- IO.realTimeInstant
              updatedGameSession <- games.modify { gameSessions =>
                gameSessions.get(id) match {
                  case Some(gameSession) if gameSession.playerId == playerId =>
                    val updatedSession = gameSession.handleCommand(command, now)
                    (gameSessions.updated(id, updatedSession), Some(updatedSession))
                  case None => (gameSessions, None)
                }
              }
              response <- updatedGameSession match {
                case Some(session) => Ok(session.asJson)
                case None => NotFound("Game session not found")
              }
            } yield response
        }
    authMiddleware(authedRoutes)
  }
}

// AuthedRoutes[UUID, IO]: indicates the routes are authenticated, and the user's identity is represented by a UUID.
// AuthedRoutes.of[UUID, IO]: creates an instance of authenticated routes where each route is associated with a UUID
//representing the user's identity.