package minesweepergame.game

import cats.effect.IO
import minesweepergame.game.Board._

import java.time.Instant

final case class GameSession(playerName: String, startTime: Instant, endTime: Option[Instant], board: Board)

object GameSession {
  def create(playerName: String, startTime: Instant, endTime: Option[Instant], level: GameLevel): IO[GameSession] = {
    for {
      board <- Board.of(level)
    } yield GameSession(playerName, startTime, endTime, board)
  }
}