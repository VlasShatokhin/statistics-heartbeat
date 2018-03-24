package io.vlas.heartbeat

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import io.vlas.heartbeat.marshalling.JsonSupport
import io.vlas.heartbeat.model.{Statistics, Transaction}
import io.vlas.heartbeat.service.TransactionStatisticsService.{Created, GetStatistics, PostTransaction, TooOld}

import scala.concurrent.ExecutionContext

trait StatisticsApi extends Directives with JsonSupport {

  def statisticsActor: ActorRef

  implicit def ec: ExecutionContext
  implicit def timeout: Timeout

  def route: Route = (path("transactions") & pathEndOrSingleSlash) {
    post {
      entity(as[Transaction]) {
        transaction =>
          onSuccess {
            statisticsActor ? PostTransaction(System.currentTimeMillis(), transaction)
          } {
            case Created => complete(StatusCodes.Created)
            case TooOld => complete(StatusCodes.NoContent)
          }
      }
    }
  } ~ (path("statistics") & pathEndOrSingleSlash) {
    get {
      onSuccess {
        (statisticsActor ? GetStatistics(System.currentTimeMillis())).mapTo[Statistics]
      } { stats =>
        complete((StatusCodes.OK, stats))
      }
    }
  }
}
