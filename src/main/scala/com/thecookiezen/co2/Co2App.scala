package com.thecookiezen.co2

import com.typesafe.config.ConfigFactory

object Co2App extends App {

  val config = ConfigFactory.load("sensor.conf");

  val thresholdLevel = config.getInt("sensor.co2_threshold_level")

  println(s"hello co2!, threshold level = $thresholdLevel")
}
