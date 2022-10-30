package RestProject

import HelperUtils.CreateLogger
import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RejectionHandler
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, ExecutionContextExecutor, Future, Promise}
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}


object LogProcessorRestServer {

  val configReference = ConfigFactory.load().getConfig("rest")
  val logger = CreateLogger(classOf[LogProcessorRestServer.type])

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  private implicit val dispatcher: ExecutionContextExecutor = system.dispatcher
  private val port = configReference.getInt("port")
  private val host = configReference.getString("host")

  def getServerPort(): Int = port

  def rejectionHandler =
    RejectionHandler.newBuilder()
      .handleNotFound { complete(StatusCodes.NotFound, "Log Message Not found")}
      .result()

  def startServer(): Unit = {

    val dateText = configReference.getString("qParamDate")
    val timeText = configReference.getString("qParamTime")
    val intervalText = configReference.getString("qParamInterval")


    val route = handleRejections(rejectionHandler) {
      get {
        parameters(dateText, timeText, intervalText) {
          (date, time, interval) => {
            val request = sendRequest(createRequest(date, time, interval))
            val response = Await.result(request._1, FiniteDuration(configReference.getInt("timeoutInSeconds"), configReference.getString("secondsText")))
            val statusCode = Await.result(request._2, FiniteDuration(configReference.getInt("timeoutInSeconds"), configReference.getString("secondsText")))
            complete(statusCode, response)
          }
        }
      }
    }

    val futures = for { bindingFuture <- Http().bindAndHandle(route, host, port)
                  waitOnFuture  <- Promise[Done].future }
    yield (bindingFuture, waitOnFuture)

    logger.info("Server started ...")

    sys.addShutdownHook{
      futures
        .flatMap(_._1.unbind())
        .onComplete(_ => system.terminate())
    }

    Await.ready(futures, Duration.Inf)

  }

  def createRequest(date: String, time: String, interval: String): HttpRequest = {
    val uri = configReference.getString("awsApiGatewayUri")
    val dateText = configReference.getString("qParamDate")
    val timeText = configReference.getString("qParamTime")
    val intervalText = configReference.getString("qParamInterval")
    HttpRequest(
      method = HttpMethods.GET,
      uri = s"${uri}?${dateText}=${date}&${timeText}=${time}&${intervalText}=${interval}"
    )
  }

  def sendRequest(request: HttpRequest): (Future[String], Future[StatusCode]) = {
    val responseFuture = Http().singleRequest {
      request
    }

    val futureData =  responseFuture
      .flatMap(_.entity.toStrict(timeout = FiniteDuration.apply(configReference.getInt("timeoutInSeconds"), configReference.getString("secondsText"))))
      .map(_.data.utf8String)

    val futureStatusCode = responseFuture.map(_.status)

    (futureData, futureStatusCode)

  }

  def main(args: Array[String]): Unit = {
    LogProcessorRestServer.startServer()
  }


}
