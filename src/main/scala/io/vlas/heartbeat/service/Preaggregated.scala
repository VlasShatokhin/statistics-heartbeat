package io.vlas.heartbeat.service

import io.vlas.heartbeat.model.Statistics

object Preaggregated {

  def apply(amount: Double): Preaggregated =
    Preaggregated(sum = amount, max = amount, min = amount, count = 1)

  def toTransactionStatistics(preaggregated: Preaggregated): Statistics =
    Statistics(
      sum = preaggregated.sum,
      avg = if (preaggregated.count > 0) preaggregated.sum / preaggregated.count else 0,
      max = preaggregated.max,
      min = preaggregated.min,
      count = preaggregated.count)
}

case class Preaggregated(sum: Double,
                                          max: Double,
                                          min: Double,
                                          count: Long) {

  def + (other: Preaggregated): Preaggregated =
    Preaggregated(
      sum = sum + other.sum,
      max = Math.max(max, other.max),
      min = Math.min(min, other.min),
      count = count + other.count)

  def + (amount: Double): Preaggregated =
    Preaggregated(
      sum = sum + amount,
      max = Math.max(amount, max),
      min = Math.min(amount, min),
      count = count + 1
    )

}
