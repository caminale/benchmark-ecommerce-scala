package dao.postgres

import java.sql.{Connection, PreparedStatement, ResultSet, Savepoint}

import dao.AProductOrderRepository
import org.slf4j.MDC
import play.api.Logger
import java.util.UUID
class PostgresProductOrderRepository(connection: Connection) extends AProductOrderRepository{

  val logger: Logger = Logger(this.getClass())

  val qGetProductOrder: String = "SELECT id FROM product_orders WHERE order_id = ? AND product_id = ? ;"
  val qInsert: String = "INSERT INTO product_orders(product_id, order_id, quantity) SELECT ?, ?, ? FROM orders WHERE state='PROCESS' and id= ? ;"
  val qUpdate: String = "UPDATE product_orders SET quantity = (SELECT quantity FROM product_orders WHERE id = ? ) + ? WHERE id = ? ;"
  val qUpdateStock: String = "UPDATE stock SET stock = (SELECT stock FROM stock WHERE product_id = ?) - ?  WHERE product_id = ? and (select state from orders where id=? ) = 'PROCESS';"
  val qGetNumberOfProductOrders: String = "SELECT SUM(quantity) FROM product_orders ;"
  val qTruncate: String = "TRUNCATE product_orders CASCADE;"

  val preparedGetProductOrderID: PreparedStatement = connection.prepareStatement(qGetProductOrder)
  val preparedUpdate: PreparedStatement = connection.prepareStatement(qUpdate)
  val preparedInsert: PreparedStatement = connection.prepareStatement(qInsert)
  val preparedUpdateStock: PreparedStatement = connection.prepareStatement(qUpdateStock)
  val preparedGetNumberOfproductOrders: PreparedStatement = connection.prepareStatement(qGetNumberOfProductOrders)
  val preparedTruncateproductOrders: PreparedStatement = connection.prepareStatement(qTruncate)


  /**
    * This is an insert Product Order, If the customer has already insert a product into his basket if he wants to
    * add this one another time, we just retrive the ProductOrder record associate ProductID and increment it quantity
    */

  override def insert(orderID: String, productID: String, quantity: Int): Int = {
    MDC.put("method", "insertProductOrder")

    preparedInsert.clearParameters()
    preparedUpdateStock.clearParameters()
    preparedGetProductOrderID.clearParameters()
    preparedUpdate.clearParameters()

    preparedGetProductOrderID.setObject(1, UUID.fromString(orderID))
    preparedGetProductOrderID.setObject(2, UUID.fromString(productID))

    preparedInsert.setObject(1, UUID.fromString(productID))
    preparedInsert.setObject(2, UUID.fromString(orderID))
    preparedInsert.setInt(3, quantity)
    preparedInsert.setObject(4, UUID.fromString(orderID))



    preparedUpdateStock.setObject(1, UUID.fromString(productID))
    preparedUpdateStock.setInt(2, quantity)
    preparedUpdateStock.setObject(3, UUID.fromString(productID))
    preparedUpdateStock.setObject(4, UUID.fromString(orderID))

    def runTransaction(sp: Savepoint, nbrRetry: Int = 0, error: String = ""): Unit = {

      if(nbrRetry > 20) throw new Exception(s"TOO MANY RETRIES + $error")

      try {

        val rsProductOrderID: ResultSet = preparedGetProductOrderID.executeQuery()

        val productOrderID: Option[String] = if(rsProductOrderID.next()) {
          if (rsProductOrderID.getObject("id").toString.isEmpty) None
          else Some(rsProductOrderID.getObject("id").toString)
        } else None

        productOrderID match {

          case Some(productOrderID) =>
            preparedUpdate.setObject(1, UUID.fromString(productOrderID))
            preparedUpdate.setInt(2, quantity)
            preparedUpdate.setObject(3, UUID.fromString(productOrderID))
            preparedUpdate.executeUpdate() match {
              case 1 =>
                preparedUpdateStock.executeUpdate()
              case 0 =>
                throw new Exception("UPDATE QUANTITY PRODUCT ORDER FAILED")
            }

          case None =>
            preparedInsert.executeUpdate() match {
              case 1 =>
                preparedUpdateStock.executeUpdate()
              case 0 =>
                throw new Exception("FAILED TO INSERT PRODUCT ORDER")
            }
        }

        preparedUpdateStock.executeUpdate() match {
          case 1 =>
            logger.debug("SUCCESS UPDATE STOCK")
          case 0 =>
            println("FAILED")
            throw new Exception("UPDATE STOCK FAILED")
        }
      } catch {
        case e: java.sql.SQLException =>
          if(e.getSQLState == "40001") {
            Thread.sleep(1000)
            connection.rollback(sp)
            runTransaction(sp, nbrRetry + 1)
          }
        case e: Exception =>
          Thread.sleep(1000)
          connection.rollback(sp)
          runTransaction(sp, nbrRetry + 1, e.getMessage)
      }
      connection.commit() //transaction block end
    }

    try {
      //Transaction block start
      connection.setAutoCommit(false)
      val sp: Savepoint = connection.setSavepoint("cockroach_restart")
      logger.debug(s"Excecute insertProductOrder(orderID: $orderID, " +
        s"productID: $productID, quantity: $quantity) | Connection :  ${connection} " +
        s"| ThreadID : ${Thread.currentThread().getName}")
      runTransaction(sp)
      logger.debug(s"Success insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) " +
        s"| Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
      1
    } catch  {
      case e =>
        logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) " +
          s"| Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} " +
          s"| Error Type : ${e.getMessage} ")
        0
    } finally {
      connection.setAutoCommit(true)
    }
  }

  override def delete(OrderID: String, productID: String, quantity: Int): Int = ???

  override def truncateProductOrder(): Int = {
    MDC.put("method", "truncateProductOrders")

    logger.debug(s"Execute truncateProductOrders() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

    preparedTruncateproductOrders.executeUpdate() match {
      case 1 =>
        logger.debug(s"Success truncateProductOrders() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
      case 0 =>
        logger.debug(s"Failed truncateProductOrders() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }

  override def getNumberOfproductOrders(): Int = {
    MDC.put("method", "getNumberOfproductOrders")

    logger.debug(s"Execute getNumberOfproductOrders() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

    val rs: ResultSet = preparedGetNumberOfproductOrders.executeQuery()

    val numberProductOrders: Int = if (rs.next()) {
      val res: Int = rs.getObject("sum").toString.toInt
      logger.info(s"Execute getNumberOfproductOrders() | NumberProductOrders : $res ")
      res
    } else 0

    numberProductOrders
  }
}