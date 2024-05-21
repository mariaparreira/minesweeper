package minesweepergame.game

import io.circe._
import io.circe.generic.semiauto._

// Defines what a square can be
case class Square(isMine: Boolean, isRevealed: Boolean, isFlagged: Boolean = false, adjacentMines: Int = 0)

object Square {
  implicit val squareCodec: Codec[Square] = deriveCodec
}