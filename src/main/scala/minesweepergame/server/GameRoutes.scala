package minesweepergame.server

import cats.effect.std.Queue
import cats.effect.{IO, Ref}
import minesweepergame.game.{GameLevel, GameSession, Uuid}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import io.circe.syntax._
import io.circe.parser._
import fs2._
import org.http4s.server.AuthMiddleware
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
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
        for {
          canJoinGame <- games.get.map { gameSessions =>
            gameSessions.get(id) match {
              case Some(session) if session.playerId == playerId => true
              case _ => false
            }
          }
          response <- if (canJoinGame) {
            for {
              queue <- Queue.unbounded[IO, WebSocketFrame]
              sendText = (text: String) => queue.offer(WebSocketFrame.Text(text))
              response <- wsb.build(
                send = Stream.repeatEval(queue.take),
                receive = _.evalMap {
                  case Text(text, _) =>
                    decode[Command](text) match {
                      case Right(command) =>
                        for {
                          now <- IO.realTimeInstant
                          updatedState <- games.modify { games =>
                            games.get(id) match {
                              case Some(game) =>
                                val updatedGame = game.handleCommand(command, now)
                                (games.updated(id, updatedGame), Some(updatedGame))
                              case None => (games, None)
                            }
                          }
                          _ <- updatedState match {
                            case Some(updated) => sendText(updated.asJson.toString)
                            case None => sendText(s"Handling command $command failed unexpectedly")
                          }
                        } yield ()
                      case Left(e) => sendText(s"Failed to parse $text as a command due to $e")
                    }
                  case  _ => IO.unit
                }
              )
            } yield response
          }
          else NotFound()

        } yield response
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