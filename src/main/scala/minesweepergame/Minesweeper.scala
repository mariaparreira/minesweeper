package minesweepergame

import scala.util.Random
import cats.effect._
import cats.implicits._


//import scala.concurrent.duration.FiniteDuration

object Minesweeper extends IOApp {
  // Define global variables to store start and end time
  private var startTime: Long = _
  private var endTime: Long = _

  // Define colors using ANSI escape codes
  private val ANSI_RESET = "\u001B[0m"
  private val ANSI_BOLD = "\u001b[1m"
  private val ANSI_BLACK = "\u001B[30m"
  private val ANSI_RED = "\u001B[31m"
  private val ANSI_GREEN = "\u001B[32m"
  private val ANSI_BLUE = "\u001B[34m"
  private val ANSI_MAGENTA = "\u001B[35m"
  private val ANSI_BRIGHT_BLUE = "\u001B[94m"
  private val ANSI_GREY = "\u001B[100m"
  private val ANSI_BRIGHT_CYAN = "\u001B[106m"

  // Defines what a square can be
  private case class Square(isMine: Boolean, isRevealed: Boolean)

  // Defines a case class to encapsulate game session information
  private final case class GameSession(playerName: String, startTime: Long, endTime: Option[Long], board: Board)

  // Start by defining a board
  private type Board = Vector[Vector[Square]]

  private def beginBoard(rows: Int, cols: Int, numMines: Int): Board = {
    val allPositions = for {
      row <- 0 until rows
      col <- 0 until cols
    } yield (row, col)

    val minePositions = Random.shuffle(allPositions).take(numMines).toSet

    val board = Vector.tabulate(rows, cols) { (row, col) =>
      val isMine = minePositions.contains((row, col))
      Square(isMine, isRevealed = false)
    }

    board
  }


  // Print the board to the console
  private def printBoard(board: Board): IO[Unit] = IO {
    // Print column numbers
    println(s"$ANSI_BOLD   " + board.head.indices.map(_.toString.padTo(3, ' ')).mkString + s"$ANSI_RESET")

    for {
      (row, rowIndex) <- board.zipWithIndex
    } yield {
      // Print row numbers and board content
      print(s"$ANSI_BOLD" + rowIndex.toString.padTo(3, ' ') + s"$ANSI_RESET")
      for {
        (square, colIndex) <- row.zipWithIndex
      } yield {
        val displayBoard = if (square.isRevealed) {
          if (square.isMine) "*"
          else {
            val adjMines = countAdjacentMines(board, rowIndex, colIndex)
            adjMines match {
              case 1 => s"$ANSI_BRIGHT_BLUE${adjMines.toString.padTo(3, ' ')}$ANSI_RESET"
              case 2 => s"$ANSI_GREEN${adjMines.toString.padTo(3, ' ')}$ANSI_RESET"
              case 3 => s"$ANSI_RED${adjMines.toString.padTo(3, ' ')}$ANSI_RESET"
              case 4 => s"$ANSI_BLUE${adjMines.toString.padTo(3, ' ')}$ANSI_RESET"
              case 5 => s"$ANSI_MAGENTA${adjMines.toString.padTo(3, ' ')}$ANSI_RESET"
              case 6 => s"$ANSI_BRIGHT_CYAN${adjMines.toString.padTo(3, ' ')}$ANSI_RESET"
              case 7 => s"$ANSI_BLACK${adjMines.toString.padTo(3, ' ')}$ANSI_RESET"
              case 8 => s"$ANSI_GREY${adjMines.toString.padTo(3, ' ')}$ANSI_RESET"
              case _ => adjMines.toString.padTo(3, ' ')
            }
          }
        } else "."
        // Adjust formatting to add space around the numbers
        val paddedNumber = displayBoard.padTo(3, ' ')
        print(paddedNumber)
      }
      println()
    }
  }

  // Reveals squares. Takes a board to return an updated board
  private def revealSquare(row: Int, col: Int, board: Board): Board = {
    // Defines a recursive helper function to reveal adjacent squares
    def revealAdjacent(row: Int, col: Int, board: Board): Board = {
      // Checks if the current square is within the bounds of the board
      if (row >= 0 && row < board.length && col >= 0 && col < board(0).length) {
        // Retrieves the square at the specified row and column
        val square = board(row)(col)

        // Checks if the square is not already revealed
        if (!square.isRevealed) {
          // Creates an updated square with isRevealed set to true
          val updatedSquare = square.copy(isRevealed = true)

          // Creates an updated row with the modified square
          val updatedRow = board(row).updated(col, updatedSquare)

          // Creates an updated board with the modified row
          val updatedBoard = board.updated(row, updatedRow)

          // If the revealed square is not a mine and has no adjacent mines, recursively reveals adjacent squares
          if (!square.isMine && countAdjacentMines(board, row, col) == 0) {
            val directions = List(
              (-1, -1), (-1, 0), (-1, 1),
              (0, -1), (0, 1),
              (1, -1), (1, 0), (1, 1)
            )

            // Recursively reveals adjacent squares
            directions.foldLeft(updatedBoard) { case (accBoard, (dx, dy)) =>
              revealAdjacent(row + dx, col + dy, accBoard)
            }
          } else updatedBoard
        } else board // If the square is already revealed, return the original board
      } else board // If the specified position is out of bounds, return the original board
    }

    // Starts revealing adjacent squares from the specified position
    revealAdjacent(row, col, board)
  }


  // Examines all possible directions where there could be a mine
  private def countAdjacentMines(board: Board, row: Int, col: Int): Int = {
    val directions = List(
      (-1, -1), (-1, 0), (-1, 1),
      (0, -1), (0, 1),
      (1, -1), (1, 0), (1, 1)
    )
    directions.map { case (dx, dy) =>
      val newRow = row + dx
      val newCol = col + dy

      if (newRow >= 0 && newRow < board.length && newCol >= 0 && newCol < board(0).length) {
        if (board(newRow)(newCol).isMine) 1 else 0
      } else 0
    }.sum
  }

  // Checks if all non-mine squares are revealed, and if true it means the player won
  private def checkWin(board: Board): Boolean = !board.flatten.exists(square => !square.isMine && !square.isRevealed)

  // Main game loop
  private def gameLoop(ref: Ref[IO, GameSession]): IO[Unit] =
    for {
      session <- ref.get
      _ <- IO.println("Enter your name: ")
      name <- IO.readLine
      _ <- IO.println(s"Welcome $name to Minesweeper!")
      _ <- IO.println("To play the game you need to insert the coordinates you want to reveal")
      _ <- IO.println("Example, 3 5 => this reveals the square in the row 3, column 5")
      _ <- IO.println("Good luck!")
      _ <- printBoard(session.board) // Print the board from the session
      _ <- IO {
        startTime = System.currentTimeMillis()
      } // Starts timer when game begins
      _ <- loop(session) // Pass only the GameSession
    } yield ()

  private def parseInput(input: String): Option[(Int, Int)] =
    input.trim.split(" ").toList.map(_.toIntOption) match {
      case Some(x) :: Some(y) :: Nil => Some((x, y))
      case _ =>
        println("Invalid input! Please enter row and column as integers separated by space.")
        None
    }

  private def loop(session: GameSession): IO[Unit] =
    for {
      _ <- IO.print("\n\nEnter row and column: ")
      input <- IO.readLine
      result = parseInput(input)
      _ <- result match {
        case Some((row, col)) if row >= 0 && row < session.board.length && col >= 0 && col < session.board(0).length =>
          if (session.board(row)(col).isMine) {
            println("\nGame Over! You hit a mine.")
            endTime = System.currentTimeMillis()
            IO.println(s"\nElapsed Time: ${(endTime - startTime) / 1000} seconds")
          } else {
            val updatedBoard = revealSquare(row, col, session.board)
            printBoard(updatedBoard) >> {
              if (checkWin(updatedBoard)) {
                println("\n\nCongratulations!! You Win!!")
                endTime = System.currentTimeMillis()
                IO.println(s"\nElapsed Time: ${(endTime - startTime) / 1000} seconds")
              }
              else loop(session.copy(board = updatedBoard))
            }
          }
        case _ =>
          loop(session)
      }
    } yield ()


  // Entry point for the app
  override def run(args: List[String]): IO[ExitCode] = {
    val rows = 8
    val cols = 8
    val numMines = 10

    val playerName = "Player"
    val startTime = System.currentTimeMillis()

    val program = for {
      ref <- Ref.of[IO, GameSession](GameSession(playerName, startTime, None, beginBoard(rows, cols, numMines)))
      _ <- gameLoop(ref)
    } yield ()

    program.as(ExitCode.Success)
  }
}