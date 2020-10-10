package com.thecookiezen.co2.domain

case class Statistics(average: Double, maxLevel: Int)

object Statistics {
  def empty: Statistics = Statistics(0, 0)
}