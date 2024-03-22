package minesweepergame.game

import cats.effect.IO
import io.circe._
import io.circe.generic.semiauto._
import minesweepergame.game.Board._
import minesweepergame.server.Command

import java.time.Instant

final case class GameSession(player: Player, startTime: Instant, endTime: Option[Instant], board: Board) {
  // Checks if the game is over, based on the time
  def gameOver: Boolean = endTime.isDefined

  // Handles a player's command (revealing a square) during the game.
  def handleCommand(command: Command, now: Instant): (GameSession, Option[GameResolution]) = {
    if (gameOver) (this, None) // Returns the current state without updating
    else {
      val updatedBoard = Board.revealSquare(command.row, command.col, board) // updates the board given the command
      val gameResolution = GameResolution.checkWin(updatedBoard) // checks if won or lost game
      val updatedTime = gameResolution match {
        case Some(GameResolution.Win(_)) | Some(GameResolution.Lose(_)) => Some(now)
        case _ => endTime
      } // if game won or lost, it'll change the endTime to the current time. otherwise, endTime will stay null.

      val updatedGame = copy(board = updatedBoard, endTime = updatedTime)
      (updatedGame, gameResolution)
    }
  }
}

object GameSession {

  implicit val gameSessionCodec: Codec[GameSession] = deriveCodec

  def create(player: Player, startTime: Instant, endTime: Option[Instant], level: GameLevel): IO[GameSession] = {
    for {
      board <- Board.of(level)
    } yield GameSession(player, startTime, endTime, board)
  }
}