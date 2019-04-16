package com.br

import scala.util.Random

object Dice extends Random {
  def roll: (Int, Int) = (self.nextInt(5) + 1, self.nextInt(5) + 1)
}