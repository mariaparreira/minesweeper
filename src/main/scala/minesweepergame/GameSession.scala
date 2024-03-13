// GameSession.scala
package minesweepergame

import minesweepergame.Board._

final case class GameSession(playerName: String, startTime: Long, endTime: Option[Long], board: Board)
