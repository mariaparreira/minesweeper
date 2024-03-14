package minesweepergame.game

import cats.effect._
import cats.implicits._
import Board._

import java.time.Instant

object Minesweeper extends IOApp {

  // Main game loop
  def gameLoop(ref: Ref[IO, GameSession]): IO[Unit] =
    for {
      session <- ref.get
      _ <- IO.println("Enter your name: ")
      name <- IO.readLine
      _ <- IO.println(s"Welcome $name to Minesweeper!")
      _ <- IO.println("\nTo play the game you need to insert the coordinates you want to reveal")
      _ <- IO.println("Example, 3 5 => this reveals the square in the row 3, column 5")
      _ <- IO.println("Good luck!\n")
      _ <- printBoard(session.board) // Print the board from the session
      _ = System.currentTimeMillis() // Starts timer when game begins
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
          if (board(row)(col).isMine) {
            println("\nGame Over! You hit a mine.")
            for {
              newEndTime <- IO.realTimeInstant
              elapsedTime = (newEndTime.toEpochMilli - startTime.toEpochMilli) / 1000
              _ <- IO.println(s"\nElapsed Time: $elapsedTime seconds")
            } yield ()
          } else {
            val updatedBoard = revealSquare(row, col, board)
            printBoard(updatedBoard) >> {
              if (checkWin(updatedBoard)) {
                println("\n\nCongratulations!! You Win!!")
                for {
                  newEndTime <- IO.realTimeInstant
                  elapsedTime = (newEndTime.toEpochMilli - startTime.toEpochMilli) / 1000
                  _ <- IO.println(s"\nElapsed Time: $elapsedTime seconds")
                } yield ()
              }
              else { // Updates the game state in the Ref
                ref.update(_.copy(board = updatedBoard)) >> loop(startTime, endTime, updatedBoard, ref)
              } // Recursively calls loop with updated game state
            }
          }
        case _ =>
          loop(startTime, endTime, board, ref) // Invalid input, prompt again
      }
    } yield ()


  // Entry point for the app
  override def run(args: List[String]): IO[ExitCode] = {

    val rows = 8
    val cols = 8
    val numMines = 10

    val playerName = "Player"

    val program = for {
      startTime <- IO.realTimeInstant
      boardFactory <- IO(BoardFactory[IO])
      initialBoard <- boardFactory.createBoard(rows, cols, numMines)
      ref <- Ref.of[IO, GameSession](GameSession(playerName, startTime, None, initialBoard))
      _ <- gameLoop(ref)
    } yield ()

    program.as(ExitCode.Success)
  }
}