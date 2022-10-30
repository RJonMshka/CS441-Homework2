import GrpcProject.{LogProcessorGRPCRestClient, LogProcessorGRPCServer}
import HelperUtils.ObtainConfigReference
import RestProject.{LogProcessorRestClient, LogProcessorRestServer}
import akka.http.scaladsl.model.HttpMethods
import com.typesafe.config.Config
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers._

class GRPCAndRestClientTest extends AnyFunSpec {
  val grpcConfigReference: Config = ObtainConfigReference("grpc") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val restConfigReference: Config = ObtainConfigReference("rest") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }

  val grpcConfig: Config = grpcConfigReference.getConfig("grpc")
  val restConfig: Config = restConfigReference.getConfig("rest")

  describe("Testing of gRPC and its HTTP Akka Client object") {

    it("Should return an httpRequest of method GET when createRequest method is called") {
      val date1 = "2022-10-25"
      val time1 = "17:00:45.190"
      val interval1 = 20

      val request = LogProcessorGRPCRestClient.createRequest(date1, time1, interval1)

      request.method shouldBe HttpMethods.GET
    }

    it("Should return an httpRequest with the provided URI") {
      val date2 = "2022-10-25"
      val time2 = "17:00:45.190"
      val interval2 = 20

      val apiUri = restConfig.getString("awsApiGatewayUri")
      val dateText = restConfig.getString("qParamDate")
      val timeText = restConfig.getString("qParamTime")
      val intervalText = restConfig.getString("qParamInterval")

      val uri = s"$apiUri?$dateText=$date2&$timeText=$time2&$intervalText=$interval2"

      val request = LogProcessorGRPCRestClient.createRequest(date2, time2, interval2)

      request.getUri().toString shouldBe uri
    }

    it("should match the right port for grpc server") {
      val port = grpcConfig.getInt("port")

      val grpcServerPort = LogProcessorGRPCServer.getServerPort

      grpcServerPort shouldBe port
    }
  }

  describe("Testing of Rest Client and Server objects") {

    it("Should return an httpRequest of method GET when createRequest of rest api method is called") {
      val date3 = "2022-10-25"
      val time3 = "17:00:45.190"
      val interval3 = 20

      val request = LogProcessorRestClient.createRequest(date3, time3, interval3)

      request.method shouldBe HttpMethods.GET
    }

    it("Should return an httpRequest with the provided rest server URI") {
      val date4 = "2022-10-25"
      val time4 = "17:00:45.190"
      val interval4 = 20

      val apiUri = restConfig.getString("serverUri")
      val dateText = restConfig.getString("qParamDate")
      val timeText = restConfig.getString("qParamTime")
      val intervalText = restConfig.getString("qParamInterval")

      val uri = s"$apiUri?$dateText=$date4&$timeText=$time4&$intervalText=$interval4"

      val request = LogProcessorRestClient.createRequest(date4, time4, interval4)

      request.getUri().toString shouldBe uri
    }

    it("Should match the right port for rest server") {
      val port = restConfig.getInt("port")

      val restServerPort = LogProcessorRestServer.getServerPort

      restServerPort shouldBe port
    }

    it("Should return an httpRequest with the provided aws api gateway URI") {
      val date5 = "2022-10-25"
      val time5 = "17:00:45.190"
      val interval5 = 20

      val apiUri = restConfig.getString("awsApiGatewayUri")
      val dateText = restConfig.getString("qParamDate")
      val timeText = restConfig.getString("qParamTime")
      val intervalText = restConfig.getString("qParamInterval")

      val uri = s"$apiUri?$dateText=$date5&$timeText=$time5&$intervalText=$interval5"

      val request = LogProcessorRestServer.createRequest(date5, time5, interval5.toString)

      request.getUri().toString shouldBe uri
    }
  }
}
