package filters

import javax.inject._
import org.slf4j.MDC
import play.api.Logger

// #essential-filter-example
import javax.inject.Inject
import akka.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.mvc._
import scala.concurrent.ExecutionContext
/**
  * This is a simple filter that adds a header to all requests. It's
  * added to the application's list of filters by the
  * [[Filters]] class.
  *
  * @param ec This class is needed to execute code asynchronously.
  * It is used below by the `map` method.
  */
@Singleton
class ExampleFilter @Inject() (implicit ec: ExecutionContext) extends EssentialFilter {
  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      MDC.put("method", "TimeBenchMark")

      val logger: Logger = Logger(this.getClass())

      val startTime = System.currentTimeMillis

      val accumulator: Accumulator[ByteString, Result] = nextFilter(requestHeader)

      accumulator.map { result =>

        val endTime = System.currentTimeMillis
        val requestTime = endTime - startTime

        logger.info(s"${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms and returned ${result.header.status}")
        result.withHeaders("Request-Time" -> requestTime.toString)

      }
    }
  }
}