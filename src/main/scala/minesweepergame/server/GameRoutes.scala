package minesweepergame.server

import cats.effect.std.Queue
import cats.effect.{IO, Ref}
import cats.implicits.toFoldableOps
import minesweepergame.game.{GameId, GameLevel, GameResolution, GameSession, Player}
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
  def apply(games: Ref[IO, Map[UUID, GameSession]], leaderBoard: Ref[IO, Map[String, Long]], authMiddleware: AuthMiddleware[IO, Player], wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {

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
                          maybeUpdateGameAndMaybeResolution <- games.modify { games =>
                            games.get(id) match {
                              case Some(game) => // If game was found
                                val (updatedGame, resolution) = game.handleCommand(command, now)
                                // If no resolution - update game, if there's one (lose/win) - remove
                                val newState = resolution.fold(games.updated(id, updatedGame))(_ => games.removed(id))
                                (newState, Some((updatedGame, resolution)))
                              case None => (games, None) // If no game found, doesn't update games ref and returns None
                            }
                          }
                          _ <- maybeUpdateGameAndMaybeResolution match {
                            case Some((updatedGame, maybeResolution)) => // This means a game was found and updated
                              for {
                                _ <- sendText(updatedGame.asJson.noSpaces)
                                _ <- maybeResolution.traverse_ { resolution =>
                                  val updateLeaderBoard = resolution match {
                                    case _: GameResolution.Win => // If win, calculates time taken
                                      val elapsedTime = now.getEpochSecond - updatedGame.startTime.getEpochSecond
                                      leaderBoard.update { leaderBoard => // and updates the LB with best time
                                        val bestTime = leaderBoard.get(player.screenName).map(_.min(elapsedTime)).getOrElse(elapsedTime)
                                        leaderBoard.updated(player.screenName, bestTime)
                                      }
                                    case _ => IO.unit
                                  }
                                  // Sends a message indicating the resolution outcome and disconnects the server
                                  updateLeaderBoard *> sendText(resolution.msg) *> queue.offer(WebSocketFrame.Close())
                                }
                              } yield ()
                          }
                        } yield ()
                      case Left(e) => sendText(s"Failed to parse $text as a command due to $e")
                    }
                  case _ => IO.unit
                }
              )
            } yield response
          }
          else NotFound()

        } yield response

      case GET -> Root / "game" / "leaderBoard" as _ =>
        for {
          leaderBoardMap <- leaderBoard.get
          top10Results = leaderBoardMap.toList.sortBy(_._2).take(10) // Get top 10 results sorted by ascending order of time
          response <- Ok(top10Results.asJson)
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
// GET -> Root / "game" / "leaderBoard" as _: retrieves the leaderboard, and orders top 10 results by ascending order,
//so from best time (shortest) to worst time (longest).

// Sets up http routes for managing game sessions in the game, including creating new sessions and
//connecting to existing ones via WebSocket, and retrieving the leaderboard for a server.