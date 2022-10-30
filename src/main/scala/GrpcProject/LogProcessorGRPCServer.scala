package GrpcProject

import HelperUtils.{CreateLogger, ObtainConfigReference}
import com.grpcLogProcessor.protos.LogProcessor.{LogProcessServiceGrpc, LogProcessorReply, LogProcessorRequest}
import com.typesafe.config.Config
import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import org.slf4j.Logger

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}

object LogProcessorGRPCServer {

  val configReference: Config = ObtainConfigReference("grpc") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val config: Config = configReference.getConfig("grpc")
  val logger: Logger = CreateLogger(classOf[LogProcessorGRPCServer.type])

  private val port = config.getInt("port")

  def getServerPort: Int = port

  def main(args: Array[String]): Unit = {
    val grpcServer = new LogProcessorGRPCServer(ExecutionContext.global)
    grpcServer.startServer()
  }

  private class LogProcessorGRPCServer(private val executionContext: ExecutionContext) {

    def startServer(): Unit = {
      val serverBuilder = NettyServerBuilder.forPort(LogProcessorGRPCServer.port)
      serverBuilder.addService(LogProcessServiceGrpc.bindService(new LogProcessServiceImpl, this.executionContext))
      val server = serverBuilder.build().start()

      logger.info(s"GRPC Server started at port: ${LogProcessorGRPCServer.port}")

      sys.addShutdownHook {
        logger.info("shutting down gRPC server since JVM is shutting down")
        this.stop(server)
        logger.info("server shut down")
      }

      this.blockUntilShutdown(server)
    }

    private def stop(server: Server): Unit = {
      if(server != null) {
        server.shutdown()
      }
    }

    private def blockUntilShutdown(server: Server): Unit = {
      if (server != null) {
        server.awaitTermination()
      }
    }

  }

  private class LogProcessServiceImpl extends LogProcessServiceGrpc.LogProcessService {

    override def processLogs(request: LogProcessorRequest): Future[LogProcessorReply] = {
      val date = request.date
      val time = request.time
      val interval = request.interval

      val httpserverResponseFutureTuple = LogProcessorGRPCRestClient.processHttpRequest(date, time, interval)

      val result = Await.result(httpserverResponseFutureTuple._1, FiniteDuration(config.getInt("timeoutInSeconds"), config.getString("secondsText")))

      val reply = LogProcessorReply(result)
      Future.successful(reply)
    }
  }

}
