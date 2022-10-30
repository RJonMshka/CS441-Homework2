package HelperUtils

import org.slf4j.{Logger, LoggerFactory}
import scala.util.{Failure, Success, Try}

/**
 * Object to create logger objects by passing the class reference
 */

object CreateLogger {
  def apply[T](class4logger: Class[T]): Logger = {
    val logBackXml = "logback.xml"
    val logger = LoggerFactory.getLogger(class4logger)
    Try(getClass.getClassLoader.getResourceAsStream(logBackXml)) match {
      case Failure(exception) => logger.error(s"Failed to locate $logBackXml for reason $exception")
      case Success(inStream) => inStream.close()
    }
    logger
  }
}
