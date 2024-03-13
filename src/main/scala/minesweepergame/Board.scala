package minesweepergame

import cats.effect.{IO, Sync}

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
  private val ANSI_BRIGHT_BLUE = "\u001B[94m"
  private val ANSI_GREY = "\u001B[100m"
  private val ANSI_BRIGHT_CYAN = "\u001B[106m"

  // Start by defining a board
  type Board = Vector[Vector[Square]]

  trait BoardFactory[F[_]] {
    def createBoard(rows: Int, cols: Int, numMines: Int): F[Board]
  }

  object BoardFactory {
    def apply[F[_] : Sync]: BoardFactory[F] = (rows: Int, cols: Int, numMines: Int) => Sync[F].delay {
      val allPositions =
        for {
          row <- 0 until rows
          col <- 0 until cols
        } yield (row, col)

      val minePositions = Random.shuffle(allPositions).take(numMines).toSet

      Vector.tabulate(rows, cols) { (row, col) =>
        val isMine = minePositions.contains((row, col))
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
      (row, rowIndex) <- board.zipWithIndex
    } yield {
      // Print row numbers and board content
      boardString.append(s"$ANSI_BOLD" + rowIndex.toString.padTo(3, ' ') + s"$ANSI_RESET")
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
        boardString.append(paddedNumber)
      }
      boardString.append("\n")
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

  // Checks if all non-mine squares are revealed, and if true it means the player won
  def checkWin(board: Board): Boolean = !board.flatten.exists(square => !square.isMine && !square.isRevealed)
}