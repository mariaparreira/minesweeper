package minesweepergame.server.authentication

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import minesweepergame.game.Uuid
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.io._
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm.HS256
import pdi.jwt.{Jwt, JwtClaim}

import java.util.UUID

object AuthRoutes {
  def apply(authSecret: String, authMiddleware: AuthMiddleware[IO, UUID]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case POST -> Root / "auth" / "login" =>
        for {
          id <- IO.randomUUID
          claim = JwtClaim(subject = Some(id.toString))
          token <- IO(Jwt.encode(claim, authSecret, HS256))
          response <- Ok(token)
        } yield response
    } <+> authMiddleware(AuthedRoutes.of[UUID, IO] {
      case GET -> Root / "auth" / "info" as id =>
        Ok(s"ID: $id")
    })
  }
}

// Provides routes for generating JWT tokens based on UUIDs and handling authenticated requests,
//to retrieve ids.