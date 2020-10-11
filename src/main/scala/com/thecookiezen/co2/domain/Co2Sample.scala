package com.thecookiezen.co2.domain

import com.thecookiezen.co2.domain.Co2Sample.Measurement
import com.thecookiezen.co2.sensor.Co2Sensor.Co2SampleReading

case class Co2Sample(utcTimestamp: Long, sample: Measurement)

object Co2Sample {
  type Measurement = Int

  val average: List[Co2Sample] => Option[Double] = { samples =>
    if (samples.isEmpty) {
      None
    } else {
      val (sum, events) = samples.foldLeft((0, 0)) {
        case ((sum, number), sample) => (sum + sample.sample, number + 1)
      }

      Some(sum.toDouble / events)
    }
  }

  val max: List[Co2Sample] => Option[Co2Sample] = _.maxByOption(_.sample)

  def fromReading(reading: Co2SampleReading): Co2Sample = Co2Sample(reading.time.toEpochSecond, reading.measurement)
}