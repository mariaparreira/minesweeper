package minesweepergame.game

import cats.effect.IO
import minesweepergame.game.Board.Board

import scala.util.Random

sealed trait GameLevel
object GameLevel {
  case object Easy extends GameLevel

  case object Medium extends GameLevel

  case object Expert extends GameLevel

  def gameLevel(level: String): Option[GameLevel] = level.toLowerCase match {
    case "easy" => Some(Easy)
    case "medium" => Some(Medium)
    case "expert" => Some(Expert)
    case _ => None
  }

  def createBoardLevel(level: GameLevel): IO[Board] = IO {
    val numRows = level match {
      case GameLevel.Easy => 8
      case GameLevel.Medium => 16
      case GameLevel.Expert => 30
    }

    val numCols = level match {
      case GameLevel.Easy => 8
      case GameLevel.Medium => 16
      case GameLevel.Expert => 16
    }

    val numMines = level match {
      case GameLevel.Easy => 10
      case GameLevel.Medium => 40
      case GameLevel.Expert => 99
    }

    val allPositions = for {
      row <- 0 until numRows
      col <- 0 until numCols
    } yield (row, col)

    val minePositions = Random.shuffle(allPositions).take(numMines).toSet

    Vector.tabulate(numRows, numCols) { (row, col) =>
      val isMine = minePositions.contains((row, col))
      Square(isMine, isRevealed = false)
    }
  }
}

