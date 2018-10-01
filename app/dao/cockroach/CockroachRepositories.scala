package dao.cockroach

import java.sql.{Connection, DriverManager}

import com.typesafe.config.ConfigFactory
import dao.{ManagerRequest, Repositories}
import org.slf4j.MDC
import play.api.Logger


class CockroachRepositories extends {

  val connection : Connection = DriverManager.getConnection(
    ConfigFactory.load().getString("dbs.cockroach.url"), "root", "")

} with Repositories(
  new CockroachProductRepository(connection),
  new CockroachProductOrderRepository(connection),
  new CockroachOrderRepository(connection),
  new CockroachAvailableProductRepository(connection),
  new CockroachCustomerRepository(connection),
  new CockroachStockRepository(connection),
  new CockroachStarterBenchRepository(connection)) {

  val logger:Logger = Logger(this.getClass)

  override def handleRequestError(exception: Exception): Unit = {
    MDC.put("method", "handleRequestError")
    logger.error(exception+"******* HANDLE ERROR *******" + Thread.currentThread().getName)
    ManagerRequest.resetThreadLocalRepositories()
  }
}
