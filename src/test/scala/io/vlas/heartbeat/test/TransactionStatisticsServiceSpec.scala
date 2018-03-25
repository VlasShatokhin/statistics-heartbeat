package io.vlas.heartbeat.test

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import io.vlas.heartbeat.model.{Statistics, Transaction}
import io.vlas.heartbeat.service.TransactionStatisticsService.{Created, GetStatistics, PostTransaction, Rejected}
import io.vlas.heartbeat.service.{Preaggregated, TransactionStatisticsService}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

object TransactionStatisticsServiceSpec {

  val ttl: Duration = 10.seconds

  val apiPostTimeMillis: Long = System.currentTimeMillis()

  val postWithinTtlMillis: Long = apiPostTimeMillis - ttl.toMillis / 2
  val postAfterTtlTMillis: Long = apiPostTimeMillis - ttl.toMillis * 2
  val futurePostMillis: Long = apiPostTimeMillis + ttl.toMillis

  val postWithinTtlSeconds: Long = postWithinTtlMillis / 1000
  val postAfterTtlSeconds: Long = postAfterTtlTMillis / 1000

  val amount = 5d
  val minAmount = 2d
  val maxAmount = 10d
}

class TransactionStatisticsServiceSpec extends TestKit(ActorSystem("TransactionStatisticsServiceSpec"))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  import TransactionStatisticsServiceSpec._

  "TransactionStatisticsService" must {


    "create the record within TTL" in {

      val statisticsActor = system.actorOf(TransactionStatisticsService.props(ttl))

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(amount, postWithinTtlMillis))

      expectMsg(Created)
    }

    "reject the record after TTL" in {

      val statisticsActor = system.actorOf(TransactionStatisticsService.props(ttl))

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(amount, postAfterTtlTMillis))

      expectMsg(Rejected)
    }

    "reject the record with future timestamp" in {

      val statisticsActor = system.actorOf(TransactionStatisticsService.props(ttl))

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(amount, futurePostMillis))

      expectMsg(Rejected)
    }

    "respond with empty stats if nothing was added" in {

      val statisticsActor = system.actorOf(TransactionStatisticsService.props(ttl))

      statisticsActor ! GetStatistics(apiPostTimeMillis)

      expectMsg(Statistics.empty)
    }


    "respond with properly calculated stats on correct records" in {

      val statisticsActor = system.actorOf(TransactionStatisticsService.props(ttl))

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(minAmount, postWithinTtlMillis))
      expectMsg(Created)

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(maxAmount, postWithinTtlMillis))
      expectMsg(Created)

      statisticsActor ! GetStatistics(apiPostTimeMillis)
      expectMsg {
        Statistics(
          sum = minAmount + maxAmount,
          avg = (minAmount + maxAmount) / 2,
          max = maxAmount,
          min = minAmount,
          count = 2)
      }
    }

    "return stats within the deadline" in {

      val map = Map(postAfterTtlSeconds -> Preaggregated(amount))
      val statisticsActor = system.actorOf(TransactionStatisticsService.props(ttl, map))

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(minAmount, postWithinTtlMillis))
      expectMsg(Created)

      statisticsActor ! GetStatistics(apiPostTimeMillis)
      expectMsg {
        Statistics(
          sum = minAmount,
          avg = minAmount,
          min = minAmount,
          max = minAmount,
          count = 1)
      }

    }
  }
}
