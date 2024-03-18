package minesweepergame.game

//import io.circe.Codec
//import io.circe.generic.semiauto.deriveCodec
import minesweepergame.game.Board._

sealed trait GameResolution

object GameResolution {

  //implicit val resoltuionCodec: Codec[GameResolution] = deriveCodec

  final case class Win(msg: String) extends GameResolution
  final case class Lose(msg: String) extends GameResolution

  def checkWin(board: Board): Option[GameResolution] = {
    val unrevealedNonMine = !board.flatten.exists(square => !square.isMine && !square.isRevealed)

    if (unrevealedNonMine) {
      Some(GameResolution.Win("Congratulations! You have won the game!")) // If all non-mine squares are revealed
    } else if (board.flatten.exists(square => square.isRevealed && square.isMine)) {
      Some(GameResolution.Lose("Game over! You have lost the game!")) // If the revealed square is a mine
    } else {
      None // Game is still ongoing, return None
    }
  }
}
