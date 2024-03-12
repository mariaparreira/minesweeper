package minesweepergame

import scala.util.Random
import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import fs2._

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

  // Start by defining a board
  type Board = Array[Array[Square]]

  def beginBoard(rows: Int, cols: Int, numMines: Int): Board = {
    val board = Array.ofDim[Square](rows, cols)

    // Places mines randomly
    var minesPlaced = 0
    while (minesPlaced < numMines) {
      val row = Random.nextInt(rows)
      val col = Random.nextInt(cols)

      if (board(row)(col) == null) {
        board(row)(col) = Square(isMine = true, isRevealed = false)
        minesPlaced += 1
      }
    }

    // Fills the rest with non-mines
    for {
      i <- 0 until rows
      j <- 0 until cols

      if board(i)(j) == null
    } yield board(i)(j) = Square(isMine = false, isRevealed = false)

    board // The board is returned
  }

  // Print the board to the console
  def printBoard(board: Board): IO[Unit] = IO {
    // Print column numbers
    println(s"${ANSI_BOLD}   " + board(0).indices.map(_.toString.padTo(3, ' ')).mkString + s"${ANSI_RESET}")

    for {
      row <- board.indices
    } yield {
      // Print row numbers and board content
      print(s"${ANSI_BOLD}" + row.toString.padTo(3, ' ') + s"${ANSI_RESET}")
      for {
        col <- board(row).indices
      } yield {
        val square = board(row)(col)
        val displayBoard = if (square.isRevealed) {
          if (square.isMine) "*"
          else {
            val adjMines = countAdjacentMines(board, row, col)
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
    // check if it's within the bounds
    def reveal(row: Int, col: Int): Unit = {
      if (row >= 0 && row < board.length && col >= 0 && col < board(0).length) {
        board(row)(col) match {
          case square@Square(_, false) => board(row)(col) = square.copy(isRevealed = true)
            if (!square.isMine && countAdjacentMines(board, row, col) == 0) {
              val directions = List(
                (-1, -1), (-1, 0), (-1, 1),
                (0, -1), (0, 1),
                (1, -1), (1, 0), (1, 1)
              )

              for {
                (dx, dy) <- directions
              } reveal(row + dx, col + dy)
            }

          case _ => // in case the square is already revealed or is a bomb, we do nothing
        }
      }
    }

    reveal(row, col)
    board
  }

  // Examines all possible directions where there could be a mine
  def countAdjacentMines(board: Board, row: Int, col: Int): Int = {
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

  def checkWin(board: Board): Boolean = !board.flatten.exists(square => !square.isMine && !square.isRevealed)

  // Main game loop
  def gameLoop(board: Board): IO[Unit] =
    for {
      _ <- IO.println("Welcome to Minesweeper!")
      _ <- IO.println("To play the game you need to insert the coordinates you want to reveal")
      _ <- IO.println("Example, 3 5 => this reveals the square in the row 3, column 5")
      _ <- IO.println("Good luck!")
      _ <- printBoard(board)
      _ <- IO { startTime = System.currentTimeMillis() } // Starts timer when game begins
      _ <- loop(board)
    } yield ()

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

  def loop(board: Board): IO[Unit] =
    for {
      _ <- IO.print("\n\nEnter row and column: ")
      input <- IO.readLine
      result = parseInput(input)
      _ <- result match {
        case Some((row, col)) if row >= 0 && row < board.length && col >= 0 && col < board(0).length =>
          if (board(row)(col).isMine) {
            println("\nGame Over! You hit a bomb.")
            endTime = System.currentTimeMillis()
            IO.println(s"\nElapsed Time: ${(endTime - startTime) / 1000} seconds")
            //IO.unit
          } else {
            val updatedBoard = revealSquare(row, col, board)
            printBoard(updatedBoard) >> {
              if (checkWin(updatedBoard)) {
                println("\n\nCongratulations!! You Win!!")
                endTime = System.currentTimeMillis()
                IO.println(s"\nElapsed Time: ${(endTime - startTime) / 1000} seconds")
              }
              else loop(updatedBoard)
            }
          }
        case _ =>
          loop(board)
      }
    } yield ()


  // Entry point for the app
  override def run(args: List[String]): IO[ExitCode] = {
    val rows = 8
    val cols = 8
    val numMines = 10

    val board = beginBoard(rows, cols, numMines)

    gameLoop(board).as(ExitCode.Success)
  }
}