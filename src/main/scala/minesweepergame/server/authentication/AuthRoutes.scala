package minesweepergame.server.authentication

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import io.circe.syntax._
import minesweepergame.game.Player
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.io._
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm.HS256
import pdi.jwt.{Jwt, JwtClaim}

import java.util.UUID

object AuthRoutes {
  def apply(authSecret: String, authMiddleware: AuthMiddleware[IO, Player]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / "auth" / "login" / screenName =>
        for {
          playerId <- IO.randomUUID // Generates random UUID
          player = Player(playerId, screenName) // Creates a player object with the provided screenName
          claim = JwtClaim(
            expiration = Some(System.currentTimeMillis() + 86400 * 1000), // Token expires in 24 hours
            subject = Some(player.asJson.noSpaces) // Includes player information in the payload
          ) // Creates a jwt claim containing the player
          token <- IO(Jwt.encode(claim, authSecret, HS256)) // encodes the jwt claim into a token using authSecret and HS256
          response <- Ok(token) // Responds with the token
        } yield response
    } <+> authMiddleware(AuthedRoutes.of[Player, IO] { // Combines the login endpoint with authenticated routes
      case GET -> Root / "auth" / "info" as player =>
        Ok(s"Player ID: $player")
    })
  }
}

// Provides routes for generating JWT tokens based on UUIDs and handling authenticated requests,
//to retrieve ids.