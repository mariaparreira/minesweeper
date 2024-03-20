package minesweepergame.server.authentication

import cats.effect.IO
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.JwtAuth
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm.HS256
import pdi.jwt.JwtClaim

import java.util.UUID
import scala.util.Try

object AuthMiddleware {
  def apply(secret: String): AuthMiddleware[IO, UUID] = {
    val auth = JwtAuth.hmac(secret, HS256) // Creates a jwt authentication instance with the provided secret and algorithm

    JwtAuthMiddleware(
      auth, authenticate = _ => (claim: JwtClaim) =>
        IO.pure(claim.subject.flatMap(sub => Try(UUID.fromString(sub)).toOption))
    )
  }
}

// Provides a middleware that can be applied to http routes to enforce jwt authentication. Extracts the user identifier
//from the jwt and provides it to downstream routes, allowing authentication and authorization logic to be applied.