package minesweepergame.game

import minesweepergame.game.Board._

sealed trait GameResolution

object GameResolution {
  final case class Win(msg: String) extends GameResolution
  final case class Lose(msg: String) extends GameResolution

  def checkWin(board: Board): GameResolution = {
    val unrevealedNonMine = board.flatten.exists(square => !square.isMine && !square.isRevealed)
    if (unrevealedNonMine) {
      GameResolution.Win("Congratulations! You have won the game!")
    } else GameResolution.Lose("Game over! You have lost the game!")
  }
}
