package minesweepergame.server

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import minesweepergame.game.GameSession
import org.http4s.ember.server._
import org.http4s.implicits._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID

object Main extends IOApp {

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      gameRef <- Ref.of[IO, Map[UUID, GameSession]](Map.empty)
      healthRoutes = HealthRoutes()
      gameRoutes = GameRoutes(gameRef)
      allRoutes = healthRoutes <+> gameRoutes
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
