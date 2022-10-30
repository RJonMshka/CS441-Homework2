package GrpcProject

import HelperUtils.CreateLogger
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCode}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.Logger

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object LogProcessorGRPCRestClient {

  val configReference: Config = ConfigFactory.load().getConfig("rest")
  val logger: Logger = CreateLogger(classOf[LogProcessorGRPCRestClient.type])

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  def processHttpRequest(date: String, time: String, interval: Int): (Future[String], Future[StatusCode]) = {
    val request = createRequest(date, time, interval)
    sendRequest(request)
  }

  def createRequest(date: String, time: String, interval: Int): HttpRequest = {
    val uri = configReference.getString("awsApiGatewayUri")
    val dateText = configReference.getString("qParamDate")
    val timeText = configReference.getString("qParamTime")
    val intervalText = configReference.getString("qParamInterval")
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
      .flatMap(_.entity.toStrict(timeout = FiniteDuration.apply(configReference.getInt("timeoutInSeconds"), configReference.getString("secondsText"))))
      .map(_.data.utf8String)

    val futureStatusCode = responseFuture.map(_.status)

    (futureData, futureStatusCode)
  }

}
