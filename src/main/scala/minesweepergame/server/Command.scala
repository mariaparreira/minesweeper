package minesweepergame.server

import io.circe._
import io.circe.generic.semiauto._

sealed trait Action
case object RevealAction extends Action
case object FlagAction extends Action

object Action {
  implicit val actionCodec: Codec[Action] = Codec.from(
    Decoder.decodeString.emap {
      case "reveal" => Right(RevealAction)
      case "flag" => Right(FlagAction)
      case other => Left(s"Unknown action: $other")
    },
    Encoder.encodeString.contramap[Action] {
      case RevealAction => "reveal"
      case FlagAction => "flag"
    }
  )
}

final case class Command(row: Int, col: Int, action: Action)

object Command {
  implicit val commandCodec: Codec[Command] = deriveCodec
}
