package com.br

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import com.br.BoardGameActor._

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex
import scala.util.{Failure, Success}

object Main {

  implicit val system: ActorSystem = ActorSystem("gg")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(3.seconds)

  lazy val log = Logging(system, classOf[BoardGameActor])

  val game: ActorRef = system.actorOf(BoardGameActor.props())

  /**
    * Regex Patterns for commands - as per spec.
    */
  val AddPlayerPattern:        Regex = "(add player) ([A-Za-z]+)".r
  val MovePlayerPatternManual: Regex  = "(move) ([A-Za-z]+) ([1-6]), ([1-6])".r
  val MovePlayerPatternAuto:   Regex  = "(move) ([A-Za-z]+)".r

  def main(args: Array[String]): Unit = {

    while (true) {

      val cmd: String = scala.io.StdIn.readLine("")

      cmd match {
        case AddPlayerPattern(_, name) =>

          val response = game.ask(AddPlayer(name))

          response.onComplete {
            case Success(value) =>
              value match {
                case PlayerAdded(players) =>
                  println(s"players: ${players.mkString(", ")}")
                case DuplicatePlayer(n) =>
                  println(s"$n: already existing player")
              }
            case Failure(exception) => log.error(s"We should handle this $exception")
          }

        case MovePlayerPatternManual(_, playerName, firstDice, secondDice) =>

          val response = game.ask(MovePlayer(playerName, firstDice.toInt, secondDice.toInt))

          response.onComplete {
            case Success(value) =>
              value match {
                case PlayerMoved(_, moves) =>
                  moves.foreach(print); println()
                case PlayerDoesNotExist(name) =>
                  println(s"$name: player does not exist")
              }
            case Failure(exception) => log.error(s"We should handle this $exception")
          }

        case MovePlayerPatternAuto(_, playerName) =>
          val rollDice = Dice.roll
          val response = game.ask(MovePlayer(playerName, rollDice._1, rollDice._2))

          response.onComplete {
            case Success(value) =>
              value match {
                case PlayerMoved(_, moves) =>
                  moves.foreach(print); println()
                case PlayerDoesNotExist(name) =>
                  println(s"$name: player does not exist")
              }
            case Failure(exception) => log.error(s"We should handle this $exception")
          }

        case _ =>
          println("Unknown command.")
      }
    }
  }
}