package minesweepergame.game

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import minesweepergame.game.Board.Board

case class GameResponse(gameId: String, board: Board)

object GameResponse {
  implicit val gameResponse: Codec[GameResponse] = deriveCodec
}
