package minesweepergame.game

import cats.effect.IO
import minesweepergame.game.Board.Board

import scala.util.Random

sealed trait GameLevel
object GameLevel {
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

