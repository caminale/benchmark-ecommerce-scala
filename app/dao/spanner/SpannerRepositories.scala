package dao.spanner

import com.google.cloud.spanner._
import dao.{ManagerRequest, Repositories}
import org.slf4j.MDC
import play.api.Logger

class SpannerRepositories extends {

  var connection: DatabaseClient = ConfigSpanner.getConnection()
  
} with Repositories(new SpannerProductRepository(connection),
                    new SpannerProductOrderRepository(connection),
                    new SpannerOrderRepository(connection),
                    new SpannerAvailableProductRepository(connection),
                    new SpannerCustomerRepository(connection),
                    new SpannerStockRepository(connection),
                    new SpannerStarterBenchRepository(connection)) {
  val logger:Logger = Logger(this.getClass)
  override def handleRequestError(exception: Exception): Unit = {
    MDC.put("method", "handleRequestError")
    logger.error(exception+"******* HANDLE ERROR *******" + Thread.currentThread().getName +" co : " +connection)
    ManagerRequest.resetThreadLocalRepositories()
  }}
