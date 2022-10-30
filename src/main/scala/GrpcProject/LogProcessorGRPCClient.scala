package GrpcProject

import HelperUtils.{CreateLogger, ObtainConfigReference}
import com.grpcLogProcessor.protos.LogProcessor.LogProcessServiceGrpc.LogProcessServiceStub
import com.grpcLogProcessor.protos.LogProcessor.{LogProcessServiceGrpc, LogProcessorReply, LogProcessorRequest}
import com.typesafe.config.Config
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object LogProcessorGRPCClient {

  val configReference: Config = ObtainConfigReference("grpc") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val config: Config = configReference.getConfig("grpc")
  val logger: Logger = CreateLogger(classOf[LogProcessorGRPCClient.type])

  def apply(host: String, port: Int): LogProcessorGRPCClient = {
    val channelBuilder = ManagedChannelBuilder.forAddress(host, port)
    channelBuilder.usePlaintext()
    val channel = channelBuilder.build()
    val stub = LogProcessServiceGrpc.stub(channel)
    new LogProcessorGRPCClient(channel, stub)
  }

  class LogProcessorGRPCClient (
    private val channel: ManagedChannel,
    private val stub: LogProcessServiceStub
                                      ) {
    def shutdown(): Unit = {
      channel.shutdown()
    }

    def processLogs(date: String, time: String, interval: Int): Future[LogProcessorReply] = {
      val grpcRequest = LogProcessorRequest(date = date, time = time, interval = interval)
      stub.processLogs(grpcRequest)
    }
  }

  def main(args: Array[String]): Unit = {
    val grpcClient = LogProcessorGRPCClient(config.getString("host"), config.getInt("port"))
    val resultsFuture = grpcClient.processLogs(config.getString("dateToProcess"), config.getString("timeToProcess"), config.getInt("intervalToProcessInSeconds"))

    resultsFuture onComplete {
      case Success(value) =>
        logger.info(s"GRPC Server Response: ${value.response}, with status code: ${value.statusCode}")
        grpcClient.shutdown()
      case Failure(exception) =>
        logger.error(exception.getMessage)
        grpcClient.shutdown()
    }

    Await.ready(resultsFuture, Duration.Inf)
  }

}
