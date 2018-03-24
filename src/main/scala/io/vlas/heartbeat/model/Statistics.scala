package io.vlas.heartbeat.model

object Statistics {
  val empty = Statistics(0, 0, 0, 0, 0)
}

case class Statistics(sum: Double,
                      avg: Double,
                      max: Double,
                      min: Double,
                      count: Long)
