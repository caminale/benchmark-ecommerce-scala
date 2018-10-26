package dao.postgres

import java.sql.{Connection, PreparedStatement, ResultSet}

import dao.AOrderRepository
import org.slf4j.MDC
import play.api.Logger
import java.util.UUID

class PostgresOrderRepository(connection: Connection)  extends AOrderRepository {

  val logger: Logger = Logger(this.getClass())

  val qInsert: String = "INSERT INTO orders(id, customer_id, state) VALUES(?, ?, ?);"
  val qValidation: String = "UPDATE orders SET state = 'VALIDATED' WHERE id = ? ;"
  val qTruncate: String = "TRUNCATE orders CASCADE;"
  val qGetNumberValidatedOrder: String = "SELECT COUNT(id) FROM orders WHERE state='VALIDATED';"


  val preparedInsertOrder : PreparedStatement = connection.prepareStatement(qInsert)
  val preparedValidationOrder : PreparedStatement = connection.prepareStatement(qValidation)
  val preparedTruncate: PreparedStatement = connection.prepareStatement(qTruncate)
  val preparedGetNumberValidatedOrder : PreparedStatement = connection.prepareStatement(qGetNumberValidatedOrder)



  override def insert(orderID: String,customerID: String): Int = {
    MDC.put("method", "insertOrder")
    preparedInsertOrder.clearParameters()
    println(orderID)
    preparedInsertOrder.setObject(1, UUID.fromString(orderID))
    preparedInsertOrder.setObject(2, UUID.fromString(customerID))
    preparedInsertOrder.setString(3, "PROCESS")

    logger.debug(s"Excecute insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

    preparedInsertOrder.executeUpdate() match {
      case 1 =>
        logger.debug(s"Success insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
      case 0 =>
        logger.error(s"Failed insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }

  override def validationOrder(orderID: String): Int = {

    MDC.put("method", "validationOrder")
    logger.debug(s"Excecute validationOrder(orderID: $orderID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    preparedValidationOrder.clearParameters()
    preparedValidationOrder.setObject(1, UUID.fromString(orderID))
    preparedValidationOrder.executeUpdate() match {
      case 1 =>
        logger.debug(s"Success validationOrder(orderID: $orderID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
      case 0 =>
        logger.error(s"Failed validationOrder(orderID: $orderID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }

  }
  override def truncateOrder(): Int = {
    MDC.put("method", "truncateOrder")
    logger.debug(s"Execute truncateOrder | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    preparedTruncate.executeUpdate() match {
      case 1 =>
        logger.debug(s"Success truncateOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
      case 0 =>
        logger.debug(s"Failed truncateOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }

  override def getNumberValidatedOrder(): Int = {
    MDC.put("method", "getNumberValidatedOrder")
    logger.debug(s"Execute getNumberValidatedOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

    val rs: ResultSet = preparedGetNumberValidatedOrder.executeQuery()

    val numberValidatedOrder: Int =  if(rs.next()) {
      val res: Int = rs.getObject("count").toString.toInt
      logger.info(s"Execute getNumberValidatedOrder() | NumberValidatedOrders : $res ")
      res
    } else 0

    numberValidatedOrder
  }
}
