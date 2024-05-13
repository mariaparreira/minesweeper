package minesweepergame.game

import cats.effect._
import cats.implicits._
import Board._
import minesweepergame.game.GameResolution.checkWin

import java.time.Instant
import scala.io.StdIn

object Minesweeper extends IOApp {

  def parseLevel(userLevel: String): IO[GameLevel] =
    GameLevel.unapply(userLevel.toLowerCase()) match {
      case Some(level) => IO.pure(level)
      case None =>
        IO.println("\nInvalid input! Please enter a valid game level (Easy, Medium or Expert):") >>
          IO.delay(StdIn.readLine()) >>= parseLevel
    }

  // Main game loop
  def gameLoop(ref: Ref[IO, GameSession]): IO[Unit] =
    for {
      session <- ref.get
      _ <- IO.println("Welcome to Minesweeper!")
      _ <- IO.println("\nTo play the game you need to insert the coordinates you want to reveal")
      _ <- IO.println("Example, 3 5 => this reveals the square in the row 3, column 5")
      _ <- IO.println("Good luck!\n")
      _ <- printBoard(session.board) // Print the board from the session
      _ = IO.realTimeInstant // Starts timer when game begins
      _ <- loop(session.startTime, session.endTime, session.board, ref)
    } yield ()

  def parseInput(input: String): Option[(Int, Int)] =
    input.trim.split(" ").toList.map(_.toIntOption) match {
      case Some(x) :: Some(y) :: Nil => Some((x, y))
      case _ =>
        println("Invalid input! Please enter row and column as integers separated by space.")
        None
    }

  def loop(startTime: Instant, endTime: Option[Instant], board: Board, ref: Ref[IO, GameSession]): IO[Unit] =
    for {
      _ <- IO.print("\n\nEnter row and column: ")
      input <- IO.readLine
      result = parseInput(input)
      _ <- result match {
        case Some((row, col)) if row >= 0 && row < board.length && col >= 0 && col < board(0).length =>
          val updatedBoard = revealSquare(row, col, board)
          printBoard(updatedBoard) >> {
            val gameResolution = checkWin(updatedBoard)
            gameResolution match {
              case Some(GameResolution.Win(_)) =>
                println("\n\nCongratulations!! You Win!!")
                for {
                  newEndTime <- IO.realTimeInstant
                  elapsedTime = (newEndTime.toEpochMilli - startTime.toEpochMilli) / 1000
                  _ <- IO.println(s"\nElapsed Time: $elapsedTime seconds")
                } yield ()
              case Some(GameResolution.Lose(_)) =>
                println("\nGame Over! You hit a mine.\n")
                // If the game is lost, reveal all bomb squares
                val updatedBoardWithBombs = revealBombs(updatedBoard)
                printBoard(updatedBoardWithBombs)
              case _ => ref.update(_.copy(board = updatedBoard)) >> loop(startTime, endTime, updatedBoard, ref)
            } // Recursively calls loop with updated game state
          }
        case _ =>
          loop(startTime, endTime, board, ref) // Invalid input, prompt again
      }
    } yield ()


  // Entry point for the app
  override def run(args: List[String]): IO[ExitCode] = {

    val playerName = "Player"

    val program = for {
      _ <- IO.println("Choose game level: Easy, Medium, Expert")
      playerId <- IO.randomUUID
      player = Player(playerName)
      userLevel <- IO.delay(StdIn.readLine())
      gameLevel <- parseLevel(userLevel)
      startTime <- IO.realTimeInstant
      initialBoard <- Board.of(gameLevel)
      ref <- Ref.of[IO, GameSession](GameSession(player, startTime, None, initialBoard))
      _ <- gameLoop(ref)
    } yield ()

    program.as(ExitCode.Success)
  }
}