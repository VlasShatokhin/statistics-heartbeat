package io.vlas.heartbeat

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.vlas.heartbeat.service.StatisticsService

import scala.concurrent.duration._

object StatisticsApp extends App
  with StatisticsApi with Context  {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val config = ConfigFactory.load()

  val recordTtl: Duration = Duration.fromNanos(config.getDuration("statistics.ttl").toNanos)
  val statisticsActor = system.actorOf(StatisticsService.props(recordTtl))

  implicit val timeout: Timeout  = Timeout(config.getDuration("http.timeout").getSeconds, TimeUnit.SECONDS)

  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  Http().bindAndHandle(route, host, port)
}
