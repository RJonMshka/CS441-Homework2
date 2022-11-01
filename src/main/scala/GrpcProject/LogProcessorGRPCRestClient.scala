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

/**
 * Object for Rest Client that interacts with API gateway
 * This client was meant to be used by GRPC Server in order to interact with API Gateway
 */
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

  /**
   * Processes HTTP Request - creates request and send it to aws api gateway endpoint
   * @param date - date to process
   * @param time - time to process
   * @param interval - interval is seconds to process
   * @return A Tuple2 of Futures, one for response string and other for status code
   */
  def processHttpRequest(date: String, time: String, interval: Int): (Future[String], Future[StatusCode]) = {
    val request = createRequest(date, time, interval)
    logger.info("processing http request")
    sendRequest(request)
  }

  /**
   * This method creates HTTP request given all the input parameters
   * @param date - date to process
   * @param time - time to process
   * @param interval - interval is seconds to process
   * @return an HTTP Request ready to be sent
   */
  def createRequest(date: String, time: String, interval: Int): HttpRequest = {
    val uri = config.getString("awsApiGatewayUri")
    val dateText = config.getString("qParamDate")
    val timeText = config.getString("qParamTime")
    val intervalText = config.getString("qParamInterval")
    logger.info("creating http request")
    HttpRequest(
      method = HttpMethods.GET,
      uri = s"$uri?$dateText=$date&$timeText=$time&$intervalText=$interval"
    )
  }

  /**
   * Sends the http request
   * @param request - http request to send
   * @return A Tuple2 of Futures, one for response string and other for status code
   */
  def sendRequest(request: HttpRequest): (Future[String], Future[StatusCode]) = {
    val responseFuture = Http().singleRequest {
      request
    }

    logger.info("sending http request")

    val futureData = responseFuture
      .flatMap(_.entity.toStrict(timeout = FiniteDuration.apply(config.getInt("timeoutInSeconds"), config.getString("secondsText"))))
      .map(_.data.utf8String)

    val futureStatusCode = responseFuture.map(_.status)

    (futureData, futureStatusCode)
  }

}
