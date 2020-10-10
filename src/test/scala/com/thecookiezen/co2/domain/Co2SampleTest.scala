package com.thecookiezen.co2.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Co2SampleTest extends AnyFlatSpec with Matchers {

  "Co2Sample" should "calculate maximum value from all samples" in {
    Co2Sample.max(List(
      Co2Sample(utcTimestamp = 0, sample = 5),
      Co2Sample(utcTimestamp = 0, sample = 2000),
      Co2Sample(utcTimestamp = 0, sample = 100)
    )).map(_.sample) shouldBe Some(2000)
  }

  it should "return None for max if there is no samples" in {
    Co2Sample.max(List.empty) shouldBe None
  }

  it should "calculate average value for all samples" in {
    Co2Sample.average(List(
      Co2Sample(utcTimestamp = 0, sample = 5),
      Co2Sample(utcTimestamp = 0, sample = 2000),
      Co2Sample(utcTimestamp = 0, sample = 100)
    )) match {
      case Some(result) => result.toInt shouldBe 701
      case None => fail()
    }
  }

  it should "return None for average if there is no samples" in {
    Co2Sample.average(List()) shouldBe None
  }
}
