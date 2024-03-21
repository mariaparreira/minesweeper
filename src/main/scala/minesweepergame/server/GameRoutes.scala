package minesweepergame.server

import cats.effect.std.Queue
import cats.effect.{IO, Ref}
import minesweepergame.game.{GameLevel, GameSession, Player, GameId}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import io.circe.syntax._
import io.circe.parser._
import io.circe._
import fs2._
import org.http4s.server.AuthMiddleware
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame._

import java.util.UUID

object GameRoutes {
  def apply(games: Ref[IO, Map[UUID, GameSession]], authMiddleware: AuthMiddleware[IO, Player], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val authedRoutes: AuthedRoutes[Player, IO] = AuthedRoutes.of[Player, IO] {
      case POST -> Root / "game" / "create" / GameLevel(level) as player =>
        for {
          instant <- IO.realTimeInstant
          newGameSession <- GameSession.create(player, instant, None, level) // creates a new GameSession and passes the playerId
          gameId <- IO.randomUUID // creates a new UUID for gameId
          _ <- games.update(_.updated(gameId, newGameSession))
          response <- Ok(s"Game created with the ID: $gameId")
        } yield response // responds with UUID

      case GET -> Root / "game" / "connect" / GameId(id) as player =>
        for {
          canJoinGame <- games.get.map(_.get(id).exists(_.player == player))
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
                                val gameOver = updatedGame.gameOver
                                val responseText = if (gameOver) {
                                  val gameJson = updatedGame.asJson
                                  val gameOverJson = Json.obj("gameOver" -> Json.True)
                                  (gameJson.deepMerge(gameOverJson)).toString
                                } else updatedGame.asJson.toString
                                val newState = {
                                  if (gameOver) games.updated(id, updatedGame).removed(id)
                                  else games.updated(id, updatedGame)
                                }
                                (newState, (responseText, gameOver))
                                //(games.updated(id, updatedGame), (responseText, gameOver))
                              case None => (games, (s"Game with ID $id not found", false))
                            }
                          }
                          _ <- updatedState match {
                            case (responseText, true) =>
                              sendText(responseText) >> queue.offer(WebSocketFrame.Close()) //gameOver = true, disconnects
                            case (responseText, _) => sendText(responseText)
                            case _ => IO.unit //sendText(s"Handling command $command failed unexpectedly")
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
// req @ POST -> Root / "game" / "command" / GameId(id) as playerId: handles post requests to the /game/command endpoint
//with a specific game session ID. Extracts the command from the request body, updates the corresponding game session
//with the command, and responds with the updated game session as JSON.
// authMiddleware(authedRoutes): applies the authentication middleware to the AuthedRoutes, ensuring that only
//authenticated requests are processed

// Sets up http routes for managing game sessions in the game, including creating new sessions and
//handling game commands, while also adding authentication to these routes using AuthMiddleware.