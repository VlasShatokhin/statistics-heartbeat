package io.vlas.heartbeat.marshalling

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.vlas.heartbeat.model.{Statistics, Transaction}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val statisticsFormat: RootJsonFormat[Statistics] = jsonFormat5(Statistics.apply)
  implicit val transactionFormat: RootJsonFormat[Transaction] = jsonFormat2(Transaction)
}
