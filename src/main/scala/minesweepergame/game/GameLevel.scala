package minesweepergame.game

import io.circe._
import io.circe.generic.semiauto._

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

// This represents the levels of difficulty of the game.
// Takes a string and matches to one of the difficulty levels.
// If it matches, returns the corresponding GameLevel wrapped in Some, if not returns None
