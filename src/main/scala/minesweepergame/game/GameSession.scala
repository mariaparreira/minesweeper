package minesweepergame.game

import cats.effect.IO
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import minesweepergame.game.Board._
import minesweepergame.server.Command

import java.time.Instant

final case class GameSession(playerName: String, startTime: Instant, endTime: Option[Instant], board: Board) {
  def handleCommand(command: Command, now: Instant): GameSession = {
    val updatedBoard = Board.revealSquare(command.row, command.col, board)
    val gameResolution = GameResolution.checkWin(updatedBoard)
    val updatedTime = gameResolution match {
      case GameResolution.Win(_) => Some(now)
      case _ => endTime
    }

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