package minesweepergame.game

import io.circe._
import io.circe.generic.semiauto._
import minesweepergame.game.Board._

sealed trait GameResolution {
  def msg: String
}

object GameResolution {

  implicit val resultCodec: Codec[GameResolution] = deriveCodec

  final case class Win(msg: String) extends GameResolution
  final case class Lose(msg: String) extends GameResolution

  def checkWin(board: Board): Option[GameResolution] = {
    val unrevealedNonMine = !board.flatten.exists(square => !square.isMine && !square.isRevealed)

    if (unrevealedNonMine) {
      Some(GameResolution.Win("Congratulations!! You Win!!")) // If all non-mine squares are revealed
    } else if (board.flatten.exists(square => square.isRevealed && square.isMine)) {
      Some(GameResolution.Lose("Game Over! You hit a mine.")) // If the revealed square is a mine
    } else {
      None // Game is still ongoing, return None
    }
  }
}
