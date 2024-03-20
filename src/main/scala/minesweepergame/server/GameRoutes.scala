package minesweepergame.server

import cats.effect.{IO, Ref}
import minesweepergame.game.{GameLevel, GameSession, Uuid}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import io.circe.syntax._
import io.circe.parser._
import org.http4s.server.AuthMiddleware
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame._

import java.util.UUID

object GameRoutes {
  def apply(games: Ref[IO, Map[UUID, GameSession]], authMiddleware: AuthMiddleware[IO, UUID], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val authedRoutes: AuthedRoutes[UUID, IO] = AuthedRoutes.of[UUID, IO] {
      case POST -> Root / "game" / "create" / GameLevel(level) as playerId =>
        for {
          instant <- IO.realTimeInstant
          newGameSession <- GameSession.create(playerId, "Player", instant, None, level) // creates a new GameSession and passes the playerId
          gameId <- IO.randomUUID // creates a new UUID for gameId
          _ <- games.update(_.updated(gameId, newGameSession))
          response <- Ok(s"Game created with the ID: $gameId")
        } yield response // responds with UUID

      case GET -> Root / "game" / "connect" / Uuid(id) as playerId =>
        val wsbService = wsb.build(
          send = games.get.map(_.get(id).map(_.asJson.noSpaces)),
          receive = {
              for {
                now <- IO.realTimeInstant
                updatedGameSession <- games.modify { gameSessions =>
                  gameSessions.get(id) match {
                    case Some(gameSession) if gameSession.playerId == playerId =>
                      val updatedSession = gameSession.handleCommand(_, now)
                      (gameSessions.updated(id, updatedSession), Some(updatedSession))
                    case _ => (gameSessions, None)
                  }
                }
                response <- updatedGameSession match {
                  case Some(session) => Ok(session.asJson)
                  case None => NotFound("Game session not found")
                }
              } yield response
          }
        )
        wsbService
    }

    authMiddleware(authedRoutes)
  }
}

// The apply method takes a mutable reference to a map of game sessions and an AuthMiddleware for authentication as
//arguments and returns an HttpRoutes[IO]. This method is used to create and configure the http routes for
//managing game sessions.
// AuthedRoutes[UUID, IO]: handles authenticated routes.
// POST -> Root / "game" / "create" / GameLevel(level) as playerId:  handles post requests to the /game/create endpoint
//with a specific GameLevel. Creates a new game session, generates a UUID for the game session ID, updates the game
//session map, and responds with the ID of the created game session.
// req @ POST -> Root / "game" / "command" / Uuid(id) as playerId: handles post requests to the /game/command endpoint
//with a specific game session ID. Extracts the command from the request body, updates the corresponding game session
//with the command, and responds with the updated game session as JSON.
// authMiddleware(authedRoutes): applies the authentication middleware to the AuthedRoutes, ensuring that only
//authenticated requests are processed

// Sets up http routes for managing game sessions in the game, including creating new sessions and
//handling game commands, while also adding authentication to these routes using AuthMiddleware.