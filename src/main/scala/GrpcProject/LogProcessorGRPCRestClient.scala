package GrpcProject

import HelperUtils.{CreateLogger, ObtainConfigReference}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCode}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.slf4j.Logger

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object LogProcessorGRPCRestClient {

  val configReference: Config = ObtainConfigReference("rest") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val config: Config = configReference.getConfig("rest")
  val logger: Logger = CreateLogger(classOf[LogProcessorGRPCRestClient.type])

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  def processHttpRequest(date: String, time: String, interval: Int): (Future[String], Future[StatusCode]) = {
    val request = createRequest(date, time, interval)
    sendRequest(request)
  }

  def createRequest(date: String, time: String, interval: Int): HttpRequest = {
    val uri = config.getString("awsApiGatewayUri")
    val dateText = config.getString("qParamDate")
    val timeText = config.getString("qParamTime")
    val intervalText = config.getString("qParamInterval")
    HttpRequest(
      method = HttpMethods.GET,
      uri = s"$uri?$dateText=$date&$timeText=$time&$intervalText=$interval"
    )
  }

  def sendRequest(request: HttpRequest): (Future[String], Future[StatusCode]) = {
    val responseFuture = Http().singleRequest {
      request
    }

    val futureData = responseFuture
      .flatMap(_.entity.toStrict(timeout = FiniteDuration.apply(config.getInt("timeoutInSeconds"), config.getString("secondsText"))))
      .map(_.data.utf8String)

    val futureStatusCode = responseFuture.map(_.status)

    (futureData, futureStatusCode)
  }

}
