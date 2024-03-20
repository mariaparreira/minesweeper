package minesweepergame.server.authentication

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

final case class AppConfig(authSecret: String)

object AppConfig {
  implicit val appConfigReader: ConfigReader[AppConfig] = deriveReader
} // Enables serialization and deserialization of Command objects to and from JSON.
