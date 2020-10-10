package com.thecookiezen.co2.domain

import com.thecookiezen.co2.domain.Co2Sample.Measurement

case class Co2Sample(utcTimestamp: Long, sample: Measurement)

object Co2Sample {
  type Measurement = Int
}