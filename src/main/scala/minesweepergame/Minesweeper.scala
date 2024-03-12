package minesweepergame

import scala.util.Random
import cats.effect._
import cats.implicits._


import scala.concurrent.duration.FiniteDuration

object Minesweeper extends IOApp {
  // Define global variables to store start and end time
  var startTime: Long = _
  var endTime: Long = _

  // Define colors using ANSI escape codes
  val ANSI_RESET = "\u001B[0m"
  val ANSI_BOLD = "\u001b[1m"
  val ANSI_BLACK = "\u001B[30m"
  val ANSI_RED = "\u001B[31m"
  val ANSI_GREEN = "\u001B[32m"
  val ANSI_BLUE = "\u001B[34m"
  val ANSI_MAGENTA = "\u001B[35m"
  val ANSI_BRIGHT_YELLOW = "\u001B[93m"
  val ANSI_BRIGHT_BLUE = "\u001B[94m"
  val ANSI_GREY = "\u001B[100m"
  val ANSI_BRIGHT_CYAN = "\u001B[106m"

  // Defines what a square can be
  case class Square(isMine: Boolean, isRevealed: Boolean)

  // Defines a case class to encapsulate game session information
  final case class GameSession(playerName: String, startTime: Long, endTime: Option[Long], board: Board)

  // Start by defining a board
  type Board = Vector[Vector[Square]]

  def beginBoard(rows: Int, cols: Int, numMines: Int): Board = {
    val board = Vector.tabulate(rows, cols) { (row, col) =>
      if (Random.nextInt(rows * cols) < numMines) Square(isMine = true, isRevealed = false)
      else Square(isMine = false, isRevealed = false)
    }
    board
  }

  // Print the board to the console
  def printBoard(board: Board): IO[Unit] = IO {
    // Print column numbers
    println(s"${ANSI_BOLD}   " + board.head.indices.map(_.toString.padTo(3, ' ')).mkString + s"${ANSI_RESET}")

    for {
      (row, rowIndex) <- board.zipWithIndex
    } yield {
      // Print row numbers and board content
      print(s"${ANSI_BOLD}" + rowIndex.toString.padTo(3, ' ') + s"${ANSI_RESET}")
      for {
        (square, colIndex) <- row.zipWithIndex
      } yield {
        val displayBoard = if (square.isRevealed) {
          if (square.isMine) "*"
          else {
            val adjMines = countAdjacentMines(board, rowIndex, colIndex)
            adjMines match {
              case 1 => s"${ANSI_BRIGHT_BLUE}${adjMines.toString.padTo(3, ' ')}${ANSI_RESET}"
              case 2 => s"${ANSI_GREEN}${adjMines.toString.padTo(3, ' ')}${ANSI_RESET}"
              case 3 => s"${ANSI_RED}${adjMines.toString.padTo(3, ' ')}${ANSI_RESET}"
              case 4 => s"${ANSI_BLUE}${adjMines.toString.padTo(3, ' ')}${ANSI_RESET}"
              case 5 => s"${ANSI_MAGENTA}${adjMines.toString.padTo(3, ' ')}${ANSI_RESET}"
              case 6 => s"${ANSI_BRIGHT_CYAN}${adjMines.toString.padTo(3, ' ')}${ANSI_RESET}"
              case 7 => s"${ANSI_BLACK}${adjMines.toString.padTo(3, ' ')}${ANSI_RESET}"
              case 8 => s"${ANSI_GREY}${adjMines.toString.padTo(3, ' ')}${ANSI_RESET}"
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
  def revealSquare(row: Int, col: Int, board: Board): Board = {
    // Define a recursive helper function to reveal adjacent squares
    def revealAdjacent(row: Int, col: Int, board: Board): Board = {
      // Check if the current square is within the bounds of the board
      if (row >= 0 && row < board.length && col >= 0 && col < board(0).length) {
        // Retrieve the square at the specified row and column
        val square = board(row)(col)

        // Check if the square is not already revealed
        if (!square.isRevealed) {
          // Create an updated square with isRevealed set to true
          val updatedSquare = square.copy(isRevealed = true)

          // Create an updated row with the modified square
          val updatedRow = board(row).updated(col, updatedSquare)

          // Create an updated board with the modified row
          val updatedBoard = board.updated(row, updatedRow)

          // If the revealed square is not a mine and has no adjacent mines, recursively reveal adjacent squares
          if (!square.isMine && countAdjacentMines(board, row, col) == 0) {
            val directions = List(
              (-1, -1), (-1, 0), (-1, 1),
              (0, -1), (0, 1),
              (1, -1), (1, 0), (1, 1)
            )

            // Recursively reveal adjacent squares
            directions.foldLeft(updatedBoard) { case (accBoard, (dx, dy)) =>
              revealAdjacent(row + dx, col + dy, accBoard)
            }
          } else updatedBoard
        } else board // If the square is already revealed, return the original board
      } else board // If the specified position is out of bounds, return the original board
    }

    // Start revealing adjacent squares from the specified position
    revealAdjacent(row, col, board)
  }


  // Examines all possible directions where there could be a mine
  def countAdjacentMines(board: Board, row: Int, col: Int): Int = {
    val directions = List(
      (-1, -1), (-1, 0), (-1, 1),
      (0, -1),           (0, 1),
      (1, -1),  (1, 0),  (1, 1)
    )
    directions.map { case (dx, dy) =>
      val newRow = row + dx
      val newCol = col + dy

      if (newRow >= 0 && newRow < board.length && newCol >= 0 && newCol < board(0).length) {
        if (board(newRow)(newCol).isMine) 1 else 0
      } else 0
    }.sum
  }

  def checkWin(board: Board): Boolean = !board.flatten.exists(square => !square.isMine && !square.isRevealed)

  // Main game loop
  def gameLoop(board: Board, session: GameSession): IO[Unit] =
    for {
      _ <- IO.println("Enter your name: ")
      name <- IO.readLine
      _ <- IO.println(s"Welcome $name to Minesweeper!")
      _ <- IO.println("To play the game you need to insert the coordinates you want to reveal")
      _ <- IO.println("Example, 3 5 => this reveals the square in the row 3, column 5")
      _ <- IO.println("Good luck!")
      _ <- printBoard(board)
      _ <- IO {
        startTime = System.currentTimeMillis()
      } // Starts timer when game begins
      _ <- loop(session)
    } yield ()

  //  def parseInput(input: String): Option[(Int, Int)] =
  //    input.trim.split(" ").toList.map(a => a.toIntOption) match {
  //      case Some(x) :: Some(y) :: Nil => Some((x,y))
  //      case _ => None
  //    }

  def parseInput(input: String): Option[(Int, Int)] = {
    val inputArray = input.trim.split(" ")
    if (inputArray.length != 2) {
      println("Invalid input! Please enter row and column as integers separated by space.")
      None
    } else {
      try {
        val Array(row, col) = inputArray.map(_.toInt)
        Some((row, col))
      } catch {
        case _: NumberFormatException =>
          println("Invalid input! Please enter row and column as integers separated by space.")
          None
      }
    }
  }

  def loop(session: GameSession): IO[Unit] =
    for {
      _ <- IO.print("\n\nEnter row and column: ")
      input <- IO.readLine
      result = parseInput(input)
      _ <- result match {
        case Some((row, col)) if row >= 0 && row < session.board.length && col >= 0 && col < session.board(0).length =>
          if (session.board(row)(col).isMine) {
            println("\nGame Over! You hit a bomb.")
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

    val board = beginBoard(rows, cols, numMines)

    val initialSession = GameSession(playerName, startTime, None, board)

    gameLoop(board, initialSession).as(ExitCode.Success)
  }
}