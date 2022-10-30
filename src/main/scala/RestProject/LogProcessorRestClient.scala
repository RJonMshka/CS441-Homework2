package RestProject

import HelperUtils.{CreateLogger, ObtainConfigReference}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.slf4j.Logger

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


/**
 * Object for Rest Client Logic
 */
object LogProcessorRestClient {
  // configs
  val configReference: Config = ObtainConfigReference("rest") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val config: Config = configReference.getConfig("rest")
  val logger: Logger = CreateLogger(classOf[LogProcessorRestClient.type])
  // important imports for Akka http
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  /**
   * This method processes HTTP request, creates one and sends it to request uri endpoint
   * @param date - date to process
   * @param time - time to process
   * @param interval - interval is seconds to process
   * @return Future of response
   */
  def processHttpRequest(date: String, time: String, interval: Int): Future[String] = {
    val request = createRequest(date, time, interval)
    sendRequest(request)
  }

  /**
   * This method creates Http Request given parameters
   * @param date - date to process
   * @param time - time to process
   * @param interval - interval is seconds to process
   * @return an Http request
   */
  def createRequest(date: String, time: String, interval: Int): HttpRequest = {
    val uri = config.getString("serverUri")
    val dateText = config.getString("qParamDate")
    val timeText = config.getString("qParamTime")
    val intervalText = config.getString("qParamInterval")
    HttpRequest(
      method = HttpMethods.GET,
      uri = s"$uri?$dateText=$date&$timeText=$time&$intervalText=$interval"
    )
  }

  /**
   * This method sends http request to an endpoint
   * @param request - http request to send
   * @return - Future of response
   */
  private def sendRequest(request: HttpRequest): Future[String] = {
    val responseFuture = Http().singleRequest {
      request
    }

    responseFuture
      .flatMap(_.entity.toStrict(timeout = FiniteDuration.apply(config.getInt("timeoutInSeconds"), config.getString("secondsText"))))
      .map(_.data.utf8String)
  }

  /**
   * Main entry point of rest client program
   * @param args - default arguments passed from cli or run configs
   */
  def main(args: Array[String]): Unit = {
    val responseFuture = LogProcessorRestClient.processHttpRequest(
      config.getString("dateToProcess"),
      config.getString("timeToProcess"),
      config.getInt("intervalToProcessInSeconds"))

    responseFuture onComplete {
      case Success(value) => logger.info(value)
      case Failure(exception) => logger.error(exception.getMessage)
    }

    Await.ready(responseFuture, Duration.Inf)
  }


}

