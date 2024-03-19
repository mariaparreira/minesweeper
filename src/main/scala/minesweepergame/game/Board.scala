package minesweepergame.game

import cats.effect.IO

import scala.util.Random

object Board {

  // Define colors using ANSI escape codes
  private val ANSI_RESET = "\u001B[0m"
  private val ANSI_BOLD = "\u001b[1m"
  private val ANSI_BLACK = "\u001B[30m"
  private val ANSI_RED = "\u001B[31m"
  private val ANSI_GREEN = "\u001B[32m"
  private val ANSI_BLUE = "\u001B[34m"
  private val ANSI_MAGENTA = "\u001B[35m"
  private val ANSI_GREY = "\u001B[90m"
  private val ANSI_BRIGHT_BLUE = "\u001B[94m"
  private val ANSI_BRIGHT_CYAN = "\u001B[96m"

  // Start by defining a board
  type Board = Vector[Vector[Square]]

  def of(level: GameLevel): IO[Board] = {

    // Calculates the sizing and number of mines in the board, based on the level
    val numRows = level match {
      case GameLevel.Easy => 8
      case GameLevel.Medium => 16
      case GameLevel.Expert => 30
    }

    val numCols = level match {
      case GameLevel.Easy => 8
      case GameLevel.Medium => 16
      case GameLevel.Expert => 16
    }

    val numMines = level match {
      case GameLevel.Easy => 10
      case GameLevel.Medium => 40
      case GameLevel.Expert => 99
    }

    // Generates all possible positions on the board, by iterating over each row and column
    val allPositions =
      for {
        row <- 0 until numRows
        col <- 0 until numCols
      } yield (row, col)

    for {
      // Selects a random subset of the positions to be mines
      mines <- IO { Random.shuffle(allPositions).take(numMines).toSet }
    } yield {
      // Creates a 2D game board
      Vector.tabulate(numRows, numCols) { (row, col) =>
        val isMine = mines.contains((row, col))
        Square(isMine, isRevealed = false)
      }
    }
  }

  // Constructs the string representation of the board
  def printBoard(board: Board): IO[Unit] = {
    val boardString = new StringBuilder()

    // Print column numbers
    boardString.append(s"$ANSI_BOLD   " + board.head.indices.map(_.toString.padTo(3, ' ')).mkString + s"$ANSI_RESET\n")

    for {
      (row, rowIndex) <- board.zipWithIndex // Iterates over each row
    } yield {
      // Print row numbers and board content
      boardString.append(s"$ANSI_BOLD" + rowIndex.toString.padTo(3, ' ') + s"$ANSI_RESET")
      for {
        (square, colIndex) <- row.zipWithIndex // Iterates over each square
      } yield {
        // Checks if the square was revealed
        val displayBoard = if (square.isRevealed) {
          // Checks if it's a mine or not
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
        } else "." // If the square is not yet revealed
        // Adjusts formatting to add space around the numbers
        val paddedNumber = displayBoard.padTo(3, ' ')
        boardString.append(paddedNumber)
      }
      boardString.append("\n") // Moves to the next row, after iterating through every square in a row.
    }

    IO.println(boardString.toString()) // Prints the constructed string with IO.println
  } // So, no side-effects

  // Reveals squares. Takes a board to return an updated board
  def revealSquare(row: Int, col: Int, board: Board): Board = {
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
              (0, -1),           (0, 1),
              (1, -1),  (1, 0),  (1, 1)
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

  // Checks if all non-mine squares are revealed, and if true it means the player won
  //def checkWin(board: Board): Boolean = !board.flatten.exists(square => !square.isMine && !square.isRevealed)
}
