package dao.postgres

import java.sql.{Connection, DriverManager}

import com.typesafe.config.ConfigFactory
import dao.{ManagerRequest, Repositories}
import org.slf4j.MDC
import play.api.Logger


class PostgresRepositories extends {

  val connection : Connection = DriverManager.getConnection(
    ConfigFactory.load().getString("dbs.postgres.url"), "root", "")

} with Repositories(
  new PostgresProductRepository(connection),
  new PostgresProductOrderRepository(connection),
  new PostgresOrderRepository(connection),
  new PostgresAvailableProductRepository(connection),
  new PostgresCustomerRepository(connection),
  new PostgresStockRepository(connection),
  new PostgresStarterBenchRepository(connection)) {

  val logger:Logger = Logger(this.getClass)

  override def handleRequestError(exception: Exception): Unit = {
    MDC.put("method", "handleRequestError")
    logger.error(exception+"******* HANDLE ERROR *******" + Thread.currentThread().getName)
    ManagerRequest.resetThreadLocalRepositories()
  }
}
