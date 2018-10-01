package dao.spanner

// model

import java.util.{ArrayList, Arrays, Collections}
import com.google.cloud.spanner.{Mutation, _}
import dao.AOrderRepository
import java.sql.Timestamp
import org.slf4j.MDC
import play.api.Logger


class SpannerOrderRepository(connection: DatabaseClient) extends AOrderRepository {
  
  val logger: Logger = Logger(this.getClass)
  
  val table = "orders"
  val columns = Arrays.asList("id", "customer_id", "ordered_date", "state")
  
  
  override def insert(orderID: String, customerID: String): Int = {
    MDC.put("method", "insertOrder")
    
    val orderedDate = new Timestamp(System.currentTimeMillis()).toString
    val state = "PROCESS"
    
    val mutation = Mutation.newInsertBuilder(table)
            .set(columns.get(0)).to(orderID)
            .set(columns.get(1)).to(customerID)
            .set(columns.get(2)).to(orderedDate)
            .set(columns.get(3)).to(state)
            .build
    
    
    try {
      logger.debug(s"Excecute insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
      connection.write(Collections.singletonList(mutation))
      logger.debug(s"Success insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
      1
    } catch {
      case _: SpannerException =>
        logger.error(s"Failed insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }
  
  override def validationOrder(orderID: String): Int = {
    MDC.put("method", "validationOrder")
    
    val mutation = Mutation.newUpdateBuilder(table)
            .set(columns.get(0)).to(orderID)
            .set(columns.get(3)).to("VALIDATED")
            .build
  
    try {
      logger.debug(s"Excecute validationOrder(orderID: $orderID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
      connection.write(Collections.singletonList(mutation))
      logger.debug(s"Success validationOrder(orderID: $orderID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
      1
    } catch {
      case _: SpannerException =>
        logger.error(s"Failed validationOrder(orderID: $orderID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }
  
  override def truncateOrder(): Int = {
    MDC.put("method", "truncateOrder")
    logger.debug(s"Excecute truncateOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    
    val mutations = new ArrayList[Mutation]
    mutations.add(Mutation.delete(table, KeySet.all))
    try {
      connection.write(mutations)
      logger.debug(s"Success truncateOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
      1
    } catch {
      case _: SpannerException =>
        logger.error(s"Failed truncateOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }
  
  override def getNumberValidatedOrder(): Int = {
    MDC.put("method", "getNumberValidatedOrder")
    logger.debug(s"Execute getNumberValidatedOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    
    val getNumberOfValidatedOrder: Statement = Statement.of("SELECt count(id) FROM orders WHERE state = 'VALIDATED'")
    val rs: ResultSet = connection.singleUse().executeQuery(getNumberOfValidatedOrder)
    
    val numberOrders: Int = if (rs.next) {
      val res: Int = rs.getLong(0).toInt
      logger.info(s"Execute getNumberValidatedOrder() | NumberValidatedOrders : $res ")
      res
    } else 0
    
    numberOrders
  }
  
}
