package io.vlas.heartbeat.service

import akka.actor.{Actor, Props}
import io.vlas.heartbeat.model.{Statistics, Transaction}

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration

object StatisticsService {

  def props(ttl: Duration): Props =
    Props(new StatisticsService(ttl, Map()))

  def props(ttl: Duration, initData: Map[Long, Preaggregated]): Props =
    Props(new StatisticsService(ttl, initData))

  case class PostTransaction(millis: Long, transaction: Transaction)
  case class GetStatistics(millis: Long)

  case object Created
  case object Rejected

  def millisToSeconds(millis: Long): Long = millis / 1000
}

/**
  * Service which calculates statistics for the `ttl` period. It uses time-slicing strategy
  * to aggregate stats per second, so stats retrieval takes near-constant O(1) complexity.
  * @param ttl - ttl for the posted records
  * @param initData - initial events data. Generally used for testing.
  */
class StatisticsService(ttl: Duration,
                        initData: Map[Long, Preaggregated]) extends Actor {

  import StatisticsService._

  /** Stores events aggregated per second in reverse order. */
  private var map: SortedMap[Long, Preaggregated] =
    TreeMap.empty(implicitly[Ordering[Long]].reverse) ++ initData

  override def receive: Receive = {

    case PostTransaction(millisPost, Transaction(amount, timestamp)) =>

      def requestWithinTtl: Boolean =
        !(millisPost - timestamp > ttl.toMillis || timestamp > millisPost)

      if (requestWithinTtl) {
        val second = millisToSeconds(timestamp)
        val updated = map.updated(second, map.get(second) match {
          case Some(preaggregated) => preaggregated + amount
          case None => Preaggregated(amount)
        })
        map = getAggregationsWithinTtl(updated, millisPost)
        sender ! Created
      } else
        sender ! Rejected

    case GetStatistics(millisGet) =>

      val statistics = getAggregationsWithinTtl(map, millisGet)
        .values.reduceOption(_ + _) match {
        case Some(preaggregated) => Preaggregated.toStatistics(preaggregated)
        case None => Statistics.empty
      }
      sender ! statistics
  }

  private def getAggregationsWithinTtl(map: SortedMap[Long, Preaggregated],
                                       requestTimeMillis: Long): SortedMap[Long, Preaggregated] = {
    val deadlineSeconds = millisToSeconds(requestTimeMillis) - ttl.toSeconds
    map takeWhile {
      case (second, _) => second > deadlineSeconds
    }
  }

}
