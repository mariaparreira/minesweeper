package minesweepergame.game

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

// Defines what a square can be
case class Square(isMine: Boolean, isRevealed: Boolean)

object Square {
  implicit val squareCodec: Codec[Square] = deriveCodec
}