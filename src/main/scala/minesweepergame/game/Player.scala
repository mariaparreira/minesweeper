package minesweepergame.game

import io.circe._
import io.circe.generic.semiauto._

import java.util.UUID

final case class Player(screenName: String)

object Player {
  implicit val playerCodec: Codec[Player] = deriveCodec
}