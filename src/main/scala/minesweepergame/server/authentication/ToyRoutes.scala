package minesweepergame.server.authentication

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import minesweepergame.game.GameId
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.io._
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm.HS256
import pdi.jwt.{Jwt, JwtClaim}

import java.util.UUID

object ToyRoutes {
  def apply(authSecret: String, authMiddleware: AuthMiddleware[IO, UUID]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / "auth" / "gameId" / GameId(id) =>
        val claim = JwtClaim(subject = Some(id.toString))
        for {
          token <- IO(Jwt.encode(claim, authSecret, HS256))
          response <- Ok(token)
        } yield response
    } <+> authMiddleware(AuthedRoutes.of[UUID, IO] {
      case GET -> Root / "auth" / "login" as gameId =>
        Ok(s"Game with ID: $gameId")
    })
  }
}
