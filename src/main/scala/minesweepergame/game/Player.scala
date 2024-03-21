package minesweepergame.game

import io.circe._
import io.circe.generic.semiauto._

import java.util.UUID

case class Player(id: UUID, screenName: String)

object Player {
  implicit val playerCodec: Codec[Player] = deriveCodec
}