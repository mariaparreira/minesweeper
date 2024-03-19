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
    val auth = JwtAuth.hmac(secret, HS256)

    JwtAuthMiddleware(
      auth, authenticate = _ => (claim: JwtClaim) =>
        IO.pure(claim.subject.flatMap(sub => Try(UUID.fromString(sub)).toOption))
    )
  }
}
