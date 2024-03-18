package minesweepergame.game

import cats.effect.IO
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import minesweepergame.game.Board._
import minesweepergame.server.Command

import java.time.Instant

final case class GameSession(playerName: String, startTime: Instant, endTime: Option[Instant], board: Board) {
  // Handles a player's command (revealing a square) during the game.
  def handleCommand(command: Command, now: Instant): GameSession = {
    val updatedBoard = Board.revealSquare(command.row, command.col, board) // updates the board given the command
    val gameResolution = GameResolution.checkWin(updatedBoard) // checks if won or lost game
    val updatedTime = gameResolution match {
      case Some(GameResolution.Win(_)) | Some(GameResolution.Lose(_)) => Some(now)
      case _ => endTime
    } // if game won or lost, it'll change the endTime to the current time. otherwise, endTime will stay null.

    copy(board = updatedBoard, endTime = updatedTime)
  }
}

object GameSession {

  implicit val gameSessionCodec: Codec[GameSession] = deriveCodec

  def create(playerName: String, startTime: Instant, endTime: Option[Instant], level: GameLevel): IO[GameSession] = {
    for {
      board <- Board.of(level)
    } yield GameSession(playerName, startTime, endTime, board)
  }
}