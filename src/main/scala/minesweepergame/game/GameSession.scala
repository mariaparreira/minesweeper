// GameSession.scala
package minesweepergame.game

import minesweepergame.game.Board._

final case class GameSession(playerName: String, startTime: Long, endTime: Option[Long], board: Board)
