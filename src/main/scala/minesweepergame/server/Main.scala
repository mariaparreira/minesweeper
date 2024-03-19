package minesweepergame.server

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import minesweepergame.game.GameSession
import minesweepergame.server.authentication.{AppConfig, AuthMiddleware, AuthRoutes}
import org.http4s.ember.server._
import org.http4s.implicits._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

import java.util.UUID

object Main extends IOApp {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- ConfigSource.default.loadF[IO, AppConfig]
      gameRef <- Ref.of[IO, Map[UUID, GameSession]](Map.empty)
      authMiddleware = AuthMiddleware(config.authSecret)
      authRoutes      = AuthRoutes(config.authSecret, authMiddleware)
      healthRoutes = HealthRoutes()
      gameRoutes = GameRoutes(gameRef, authMiddleware)
      allRoutes = healthRoutes <+> gameRoutes  <+> authRoutes
      server <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8000")
        .withHttpApp(allRoutes.orNotFound)
        .build
        .useForever
        .as(ExitCode.Success)
    } yield server
}

// Loads server configuration.
// Sets up game session storage.
// Defines routes for various functionalities, including health checks, game operations and authentication.
// Starts the http server to serve those routes.