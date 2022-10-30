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

object LogProcessorRestClient {

  val configReference: Config = ObtainConfigReference("rest") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val config: Config = configReference.getConfig("rest")
  val logger: Logger = CreateLogger(classOf[LogProcessorRestClient.type])

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  def processHttpRequest(date: String, time: String, interval: Int): Future[String] = {
    val request = createRequest(date, time, interval)
    sendRequest(request)
  }

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


  private def sendRequest(request: HttpRequest): Future[String] = {
    val responseFuture = Http().singleRequest {
      request
    }

    responseFuture
      .flatMap(_.entity.toStrict(timeout = FiniteDuration.apply(config.getInt("timeoutInSeconds"), config.getString("secondsText"))))
      .map(_.data.utf8String)
  }

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

