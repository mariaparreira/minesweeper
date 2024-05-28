package minesweepergame.game

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class CreatePlayerRequest(playerName: String)

object CreatePlayerRequest {
  implicit val codec: Codec[CreatePlayerRequest] = deriveCodec
}

