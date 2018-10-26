package dao.postgres

import java.sql.{Connection, PreparedStatement}

import dao.AStockRepository
import org.slf4j.MDC
import play.api.Logger


class PostgresStockRepository(connection: Connection) extends AStockRepository{

  val logger: Logger = Logger(this.getClass())
  val qDropTableStock: String = "DROP TABLE stock CASCADE;"
  val qDropTableProduct: String = "DROP TABLE product CASCADE;"
  val qRestoreDatabase: String = "RESTORE octo.stock, octo.product from 'gs://bench-octo/database-octo';"
  val preparedDropTableStock: PreparedStatement = connection.prepareStatement(qDropTableStock)
  val preparedDropTableProduct: PreparedStatement = connection.prepareStatement(qDropTableProduct)
  val preparedRestoreDatabase: PreparedStatement = connection.prepareStatement(qRestoreDatabase)

  override def updateAllStockQuantity(): Int = {
    MDC.put("method", "updateAllStockQuantity")

    logger.debug(s"Excecute updateAllStockQuantity() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
/*    try{
      preparedDropTableStock.executeUpdate()
    } catch {
      case e =>
        println(s"error from drop stock $e")
    }
    try{
      preparedDropTableProduct.executeUpdate()
    } catch {
      case e =>
        println(s"error from drop product $e")
    }
    Thread.sleep(60000)
    try {
      preparedRestoreDatabase.executeUpdate()
    }catch{
      case e =>
        println(s"error from restoresb $e"); 0
    }*/
    1
  }
}
