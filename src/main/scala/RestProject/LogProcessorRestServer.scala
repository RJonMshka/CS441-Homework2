package RestProject

import HelperUtils.{CreateLogger, ObtainConfigReference}
import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RejectionHandler
import com.typesafe.config.Config
import org.slf4j.Logger

import scala.concurrent.{Await, ExecutionContextExecutor, Future, Promise}
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration


object LogProcessorRestServer {

  val configReference: Config = ObtainConfigReference("rest") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val config: Config = configReference.getConfig("rest")
  val logger: Logger = CreateLogger(classOf[LogProcessorRestServer.type])

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val dispatcher: ExecutionContextExecutor = system.dispatcher
  private val port = config.getInt("port")
  private val host = config.getString("host")

  def getServerPort: Int = port

  def rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handleNotFound { complete(StatusCodes.NotFound, "Log Message Not found")}
      .result()

  def startServer(): Unit = {

    val dateText = config.getString("qParamDate")
    val timeText = config.getString("qParamTime")
    val intervalText = config.getString("qParamInterval")


    val route = handleRejections(rejectionHandler) {
      get {
        parameters(dateText, timeText, intervalText) {
          (date, time, interval) => {
            val request = sendRequest(createRequest(date, time, interval))
            val response = Await.result(request._1, FiniteDuration(config.getInt("timeoutInSeconds"), config.getString("secondsText")))
            val statusCode = Await.result(request._2, FiniteDuration(config.getInt("timeoutInSeconds"), config.getString("secondsText")))
            complete(statusCode, response)
          }
        }
      }
    }

    val bindingFuture = Http().bindAndHandle(route, host, port)
    val doneFuture = Promise[Done].future

    logger.info(s"Server started at port $port")

    sys.addShutdownHook{
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    }

    Await.ready(doneFuture, Duration.Inf)

  }

  def createRequest(date: String, time: String, interval: String): HttpRequest = {
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

    val futureData =  responseFuture
      .flatMap(_.entity.toStrict(timeout = FiniteDuration.apply(config.getInt("timeoutInSeconds"), config.getString("secondsText"))))
      .map(_.data.utf8String)

    val futureStatusCode = responseFuture.map(_.status)

    (futureData, futureStatusCode)

  }

  def main(args: Array[String]): Unit = {
    LogProcessorRestServer.startServer()
  }


}
