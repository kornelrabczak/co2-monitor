package com.thecookiezen.co2.domain

import com.thecookiezen.co2.domain.Co2Sample.Measurement

case class AlertLog(startTime: Long, endTime: Option[Long] = None, measurements: List[Measurement])