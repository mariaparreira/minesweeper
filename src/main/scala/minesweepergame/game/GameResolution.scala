package minesweepergame.game

import minesweepergame.game.Board._

sealed trait GameResolution

object GameResolution {
  final case class Win(msg: String) extends GameResolution
  final case class Lose(msg: String) extends GameResolution

  def checkWin(board: Board): GameResolution = {
    val unrevealedNonMine = !board.flatten.exists(square => !square.isMine && !square.isRevealed)
    // iterates through all of the elements in the board and checks if any square meets the condition in the exists
    // "board.flatten": flattens the 2D board into 1D collection. converts from a list of rows (each being
    //a list of squares) to a single list of squares.
    // ".exists(...)": tests whether at least one element of the list satisfies the predicate and returns true, which
    //means the game is still going.
    // "square => !square.isMine && !square.isRevealed": predicate. for each square checks if it's not a mine and
    //not revealed.
    // The "!" means the expression is true, if there are no unrevealed non-mine squares left on the board, indicating
    //that all non-mine squares have been revealed or all revealed squares are mines. And returns false, if there are
    //still unrevealed non-mine squares on the board.

    if (unrevealedNonMine) {
      GameResolution.Win("Congratulations! You have won the game!")
    } else GameResolution.Lose("Game over! You have lost the game!")
  }
}
