package io.vlas.heartbeat.service

import akka.actor.{Actor, Props}
import io.vlas.heartbeat.model.{Statistics, Transaction}

import scala.collection.mutable
import scala.concurrent.duration.Duration

object TransactionStatisticsService {

  val keyOrdering: Ordering[Long] = implicitly[Ordering[Long]].reverse

  def props(ttl: Duration): Props =
    Props(new TransactionStatisticsService(ttl, Map()))

  def props(ttl: Duration, map: Map[Long, Preaggregated]): Props =
    Props(new TransactionStatisticsService(ttl, map))

  case class PostTransaction(millis: Long, transaction: Transaction)
  case class GetStatistics(millis: Long)

  case object Created
  case object Rejected

  def millisToSeconds(millis: Long): Long = millis / 1000
}

class TransactionStatisticsService(ttl: Duration,
                                   initData: Map[Long, Preaggregated]) extends Actor {

  import TransactionStatisticsService._

  private var map: mutable.SortedMap[Long, Preaggregated] = mutable.TreeMap.empty(keyOrdering) ++ initData

  override def receive: Receive = {

    case PostTransaction(millisPost, Transaction(amount, timestamp)) =>

      def requestWithinTtl: Boolean =
        !(millisPost - timestamp > ttl.toMillis || timestamp > millisPost)

      if (requestWithinTtl) {
        val second = millisToSeconds(timestamp)
        this.synchronized {
          map(second) = map.get(second) match {
            case Some(preaggregated) => preaggregated + amount
            case None => Preaggregated(amount)
          }
          map = getAggregationsWithinDeadline(millisPost)
        }
        sender ! Created
      } else sender ! Rejected

    case GetStatistics(millisGet) =>

      val statsWithinTimeframe = getAggregationsWithinDeadline(millisGet)

      val statistics = statsWithinTimeframe.values.reduceOption(_ + _) match {
        case Some(aggregated) => Preaggregated.toTransactionStatistics(aggregated)
        case None => Statistics.empty
      }
      sender ! statistics
  }

  private def getAggregationsWithinDeadline(requestTimeMillis: Long): mutable.SortedMap[Long, Preaggregated] = {
    val deadlineSeconds = millisToSeconds(requestTimeMillis) - ttl.toSeconds
    map takeWhile {
      case (second, _) => second > deadlineSeconds
    }
  }

}
