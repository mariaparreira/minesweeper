package minesweepergame.server

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Command(row: Int, col: Int)

object Command {
  implicit val commandCodec: Codec[Command] = deriveCodec
} // Enables serialization and deserialization of Command objects to and from JSON.
