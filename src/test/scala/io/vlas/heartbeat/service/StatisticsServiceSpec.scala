package io.vlas.heartbeat.service

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import io.vlas.heartbeat.model.{Statistics, Transaction}
import io.vlas.heartbeat.service.StatisticsService.{Created, GetStatistics, PostTransaction, Rejected}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

object StatisticsServiceSpec {

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

class StatisticsServiceSpec extends TestKit(ActorSystem("StatisticsServiceSpec"))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  import StatisticsServiceSpec._

  "TransactionStatisticsService" must {

    "create the record within TTL" in {

      val statisticsActor = system.actorOf(StatisticsService.props(ttl))

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(amount, postWithinTtlMillis))

      expectMsg(Created)
    }

    "reject the record after TTL" in {

      val statisticsActor = system.actorOf(StatisticsService.props(ttl))

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(amount, postAfterTtlTMillis))

      expectMsg(Rejected)
    }

    "reject the record with future timestamp" in {

      val statisticsActor = system.actorOf(StatisticsService.props(ttl))

      statisticsActor ! PostTransaction(apiPostTimeMillis, Transaction(amount, futurePostMillis))

      expectMsg(Rejected)
    }

    "respond with empty stats if nothing was added" in {

      val statisticsActor = system.actorOf(StatisticsService.props(ttl))

      statisticsActor ! GetStatistics(apiPostTimeMillis)

      expectMsg(Statistics.empty)
    }

    "respond with properly calculated stats on correct records" in {

      val statisticsActor = system.actorOf(StatisticsService.props(ttl))

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

    "return stats within the ttl only" in {

      val oldData = Map(postAfterTtlSeconds -> Preaggregated(amount))
      val statisticsActor = system.actorOf(StatisticsService.props(ttl, oldData))

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
