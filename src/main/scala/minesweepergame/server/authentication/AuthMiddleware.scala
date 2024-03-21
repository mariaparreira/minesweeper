package minesweepergame.server.authentication

import cats.effect.IO
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.JwtAuth
import minesweepergame.game.Player
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm.HS256
import pdi.jwt.JwtClaim
import io.circe.parser._

object AuthMiddleware {
  def apply(secret: String): AuthMiddleware[IO, Player] = {
    val auth = JwtAuth.hmac(secret, HS256)
    // Creates a jwt authentication instance with the provided secret and algorithm

    JwtAuthMiddleware(
      auth, authenticate = _ => (claim: JwtClaim) =>
        IO.pure(claim.subject.flatMap(sub => decode[Player](sub).toOption))
    )
  }
}

// Provides a middleware that can be applied to http routes to enforce jwt authentication. Extracts the user identifier
//from the jwt and provides it to downstream routes, allowing authentication and authorization logic to be applied.