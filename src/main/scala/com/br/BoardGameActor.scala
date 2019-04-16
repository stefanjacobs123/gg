package com.br

import akka.actor.{Actor, ActorLogging, Props}
import com.br.BoardGameActor._

import scala.annotation.tailrec

sealed trait Command
sealed trait Response
sealed trait Error

object BoardGameActor {

  private val goose: Seq[Int] = Seq(5, 9, 14, 18, 23, 27)
  private val bridge: Int = 6
  private val bridgeAmount: Int = 12
  private val winAmount: Int = 63

  case class AddPlayer(playerName: String)         extends Command
  case class PlayerAdded(playerNames: Seq[String]) extends Response
  case class DuplicatePlayer(playerName: String)   extends Error

  case class MovePlayer(playerName: String, diceOne: Int, diceTwo: Int) extends Command
  case class PlayerMoved(playerName: String, moves: Seq[Move])          extends Response
  case class PlayerDoesNotExist(playerName: String)                     extends Error

  /**
    * A [[Move]] contains information on how a player moved from a position on the board to a new position on the board
    * given two die amounts.
    *
    * Its toString method also contains the business logic as to how to print the player's moves out onto the cmd-line.
    *
    * @param playerName playerName
    * @param fromPosition starting position of player
    * @param toPosition end position after move
    * @param dice (diceAmount1, diceAmount2)
    */
  case class Move(playerName: String, fromPosition: Int, toPosition: Int, dice: (Int, Int)) {

    def to:   String = if(toPosition == bridge) "The Bridge" else s"$toPosition"
    def from: String = if(fromPosition == 0)    "Start"      else s"$fromPosition"

    val defaultS:         String = s"$playerName rolls ${dice._1}, ${dice._2}. $playerName moves from $from to $to."
    val jumpS:            String = s"$playerName rolls ${dice._1}, ${dice._2}. $playerName moves from $from to $to. $playerName jumps to $bridgeAmount."
    val overflowS:        String = s"$playerName rolls ${dice._1}, ${dice._2}. $playerName moves from $from to $winAmount. $playerName bounces! $playerName returns to $to."
    val winsS:            String = s"$playerName rolls ${dice._1}, ${dice._2}. $playerName moves from $from to $winAmount. $playerName Wins!!"
    val movesFromGoose:   String = s"$playerName moves again and goes to $to."
    val movesFromToGoose: String = s"$playerName moves again and goes to $to, The Goose. "
    val movesToGoose :    String = s"$playerName rolls ${dice._1}, ${dice._2}. $playerName moves from $from to $to, The Goose. "

    override def toString: String =
      if (goose.contains(toPosition) && goose.contains(fromPosition)) movesFromToGoose
      else if (goose.contains(fromPosition))                          movesFromGoose
      else if (goose.contains(toPosition))                            movesToGoose
      else if (toPosition == bridge)                                  jumpS
      else if (fromPosition + dice._1 + dice._2 > winAmount)          overflowS
      else if (toPosition == winAmount)                               winsS
      else                                                            defaultS
  }

  /**
    * The [[Board]] contains the positions of each player on the board.
    *
    * Its member functions will either return an [[Error]] or a new instance of itself with the latest positions
    * of all active players. It's the user's responsibility to update the state of the BoardActor with given return.
    *
    * @param playersPositions Map(playerName -> playerPosition)
    */
  case class Board(playersPositions: Map[String, Int]) {
    /**
      * Add player to the board.
      *
      * @param name playerName
      * @param position position to start at (defaults to 0)
      * @return Either a DuplicatePlayer error or a new Board with player added
      */
    def add(name: String, position: Int = 0): Either[DuplicatePlayer, Board] = {
      if (playersPositions.get(name).isDefined) Left(DuplicatePlayer(name))
      else Right(Board(playersPositions.updated(name, position)))
    }

    /**
      * Move player on board.
      *
      * Recursively accumulate [[Move]]s made and keep Board on which moves are made, up to date.
      *
      * Recursive [[movePlayer]] calls are made when a player lands on a Goose.
      *
      * @param name playerName
      * @param diceOne amount on first dice
      * @param diceTwo amount on second dice
      * @param moves a sequence of [[Move]]s made
      * @param board a Map containing the latest positions of each player - playerName -> playerPosition
      * @return Either a DuplicatePlayer error or a new Board with player added
      */
    @tailrec
    final def movePlayer(name: String, diceOne: Int, diceTwo: Int, moves: Seq[Move], board: Board): Either[PlayerDoesNotExist, (Board, PlayerMoved)] = {
      if (board.playersPositions.get(name).isDefined) {
        val currentPos: Int = board.playersPositions(name)
        val nextPosition: Int = move(currentPos, diceOne + diceTwo)

        if (goose.contains(nextPosition))
          movePlayer(
            name,
            diceOne,
            diceTwo,
            moves.++(Seq(Move(name, currentPos, nextPosition, (diceOne, diceTwo)))),
            Board(board.playersPositions.updated(name, nextPosition))
          )
        else if (nextPosition == bridge)
          Right(
            Board(board.playersPositions.updated(name, bridgeAmount)),
            PlayerMoved(name, Seq(Move(name, currentPos, nextPosition, (diceOne, diceTwo))))
          )
        else if (!goose.contains(nextPosition) && moves.isEmpty)
          Right(
            Board(board.playersPositions.updated(name, nextPosition)),
            PlayerMoved(name, Seq(Move(name, currentPos, nextPosition, (diceOne, diceTwo))))
          )
        else
          Right(
            Board(board.playersPositions.updated(name, nextPosition)),
            PlayerMoved(name, moves.++(Seq(Move(name, currentPos, nextPosition, (diceOne, diceTwo)))))
          )
      }
      else {
        Left(PlayerDoesNotExist(name))
      }
    }

    /**
      * Given the currentPosition of a player on the board, return the next position after moved a given amount.
      *
      * The game dictates, that the amount to be moved ... WIP
      *
      * @param currentPosition current position on the board
      * @param moveAmmount     amount to be move
      * @param winAmmount      amount required to win
      * @return The nextPosition for a certain board position and amount to move
      */
    private def move(currentPosition: Int, moveAmmount: Int, winAmount: Int = winAmount): Int = {
      val sum = currentPosition + moveAmmount
      if (sum > winAmount) winAmount - (sum - winAmount)
      else sum
    }

  }

  def props(): Props = Props[BoardGameActor]

}

class BoardGameActor extends Actor with ActorLogging {

  // use stateful actor in order to not use mutable vars
  def receive: Receive = active(Board(Map.empty))

  def active(board: Board): Receive = {
    // add player at position 0 and return players on board
    // reply with Duplicate player error if player already exist
    case AddPlayer(playerName) =>
      val attemptAddingPlayer = board.add(playerName)

      attemptAddingPlayer match {
        case Left(duplicatePlayer) =>
          sender ! duplicatePlayer
        case Right(b: Board) =>
          context become active(b) // update board/state with player added
          sender ! PlayerAdded(b.playersPositions.keys.toSeq)
      }
    // move player the two dice amounts
    // reply with how player moved: fromPosition -> toPosition
    case MovePlayer(playerName, diceOne, diceTwo) =>
      val attemptMovingPlayer = board.movePlayer(playerName, diceOne, diceTwo, Seq.empty, board)

      attemptMovingPlayer match {
        case Left(playerDoesNotExist) =>
          sender ! playerDoesNotExist
        case Right((b: Board, playerMoved: PlayerMoved)) =>
          context become active(b) // update board/state with player moved
          sender ! playerMoved
      }

  }
}
