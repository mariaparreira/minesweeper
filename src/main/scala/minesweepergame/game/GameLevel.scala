package minesweepergame.game

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.util.Random

sealed trait GameLevel
object GameLevel {

  implicit val gameLevelCodec: Codec[GameLevel] = deriveCodec

  case object Easy extends GameLevel

  case object Medium extends GameLevel

  case object Expert extends GameLevel

  def unapply(s: String): Option[GameLevel] = s.toLowerCase match {
    case "easy" => Some(Easy)
    case "medium" => Some(Medium)
    case "expert" => Some(Expert)
    case _ => None
  }
}

