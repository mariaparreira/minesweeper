package minesweepergame.game

import cats.effect.IO
import minesweepergame.game.Board._

final case class GameSession(playerName: String, startTime: Long, endTime: Option[Long], board: Board)

object GameSession {
  def create(playerName: String, startTime: Long, endTime: Option[Long], level: GameLevel): IO[GameSession] = {
    for {
      board <- GameLevel.createBoardLevel(level)
    } yield GameSession(playerName, startTime, endTime, board)
  }
}