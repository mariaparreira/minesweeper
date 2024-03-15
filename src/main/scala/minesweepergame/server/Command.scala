package minesweepergame.server

import cats.effect.IO
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import minesweepergame.game.GameSession

final case class Command(row: Int, col: Int)

object Command {
  implicit val commandCodec: Codec[Command] = deriveCodec
}
