package minesweepergame.game

import java.util.UUID
import scala.util.Try

object GameId {

  def unapply(s: String): Option[UUID] =
    Try(UUID.fromString(s)).toOption
}

// Takes a string and attempts to parse it into a UUID.
// If successful: returns Some(UUID), if not: returns None (in case the UUID format is invalid)
