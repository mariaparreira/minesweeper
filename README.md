# MINESWEEPER

# How to Play

_This minesweeper game is 1 player and has 3 difficulty levels (easy, medium and expert)._
_The user should run the code and play on the console or can test it on the server side._

## - - - - - Console - - - - -
Run Minesweeper.scala file.

Once the game shows on the console, it will be prompted for the player to enter coordinates, first row and then column with a space in between.

If the player misspells, the input will be invalid and the player will be prompted again to enter coordinates.

To win the game, the player just needs to follow the numbers, to not touch the mines.

## - - - - - Server - - - - -
Run Main.scala file.

Once the server is running, use the following routes by order:

- POST 0.0.0.0:8000/auth/login : this will send a JWT token, which will authorize us to create a game.
- POST 0.0.0.0:8000/game/create/<difficulty_level> : we create the game with the given level of difficulty (easy, medium, expert) and it gives us a game id.
- ws://0.0.0.0:8000/game/connect/<game_id> : we connect to the game with the game ID we received and we can play the game.

When the game is over (win or lose), the id is removed and the server disconnects. 