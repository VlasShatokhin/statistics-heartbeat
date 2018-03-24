package io.vlas.heartbeat.service

import akka.actor.{Actor, Props}
import io.vlas.heartbeat.model.{Statistics, Transaction}
import io.vlas.heartbeat.service.TransactionStatisticsService.{Created, GetStatistics, PostTransaction, TooOld, millisToSeconds}

import scala.collection.mutable
import scala.concurrent.duration.Duration

object TransactionStatisticsService {

  def props(ttl: Duration): Props =
    Props(new TransactionStatisticsService(ttl, mutable.TreeMap.empty))

  def props(ttl: Duration, map: mutable.TreeMap[Long, Preaggregated]): Props =
    Props(new TransactionStatisticsService(ttl, map))

  case class PostTransaction(millis: Long, transaction: Transaction)
  case class GetStatistics(millis: Long)

  case object Created
  case object TooOld

  def millisToSeconds(millis: Long): Long = millis / 1000
}

class TransactionStatisticsService(ttl: Duration,
                                   var map: mutable.TreeMap[Long, Preaggregated]) extends Actor {

  override def receive: Receive = {

    case PostTransaction(millisPost, Transaction(amount, timestamp)) =>

      if (millisPost - timestamp > ttl.toMillis) sender ! TooOld
      else {
        val second = millisToSeconds(timestamp)
        this.synchronized {
          map(second) = map.get(second) match {
            case Some(preaggregated) => preaggregated + amount
            case None => Preaggregated(amount)
          }
          map = getAggregationsWithinDeadline(millisPost)
        }
        sender ! Created
      }

    case GetStatistics(millisGet) =>

      val statsWithinTimeframe = getAggregationsWithinDeadline(millisGet)

      val statistics = statsWithinTimeframe.values.reduceOption(_ + _) match {
        case Some(aggregated) => Preaggregated.toTransactionStatistics(aggregated)
        case None => Statistics.empty
      }
      sender ! statistics
  }

  protected def getAggregationsWithinDeadline(requestTimeMillis: Long): mutable.TreeMap[Long, Preaggregated] = {
    val deadlineSeconds = millisToSeconds(requestTimeMillis) - ttl.toSeconds
    map takeWhile {
      case (second, _) => second > deadlineSeconds
    }
  }

}
