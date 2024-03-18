package minesweepergame.game

import java.util.UUID
import scala.util.Try

object GameId {

  def unapply(s: String): Option[UUID] =
    Try(UUID.fromString(s)).toOption
}
