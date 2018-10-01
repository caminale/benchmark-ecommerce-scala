package dao.spanner

import java.util.{ArrayList, Arrays, Collection}

import com.google.cloud.spanner._
import dao.AProductOrderRepository
import models.ProductOrder
import org.slf4j.MDC
import play.api.Logger


class SpannerProductOrderRepository(connection: DatabaseClient) extends AProductOrderRepository {


  val logger: Logger = Logger(this.getClass)


  val table = "product_orders"
  val columns = Arrays.asList("order_id", "product_id", "quantity")


  override def insert(orderID: String, productID: String, quantity: Int): Int = {
    MDC.put("method", "insertProductOrderMain")


    val productOrders = ProductOrder("id", productID, orderID, quantity)

    val manager = connection.transactionManager

    var txn = manager.begin()

    def runTransaction(productOrders: ProductOrder, nbRetry: Int): Int = {
      MDC.put("method", "insertProductOrder")
      if (nbRetry > 5) {
        logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | Error Type : RetryTooOld")
        return 0
      }

      // Get order state
      val stateOrder: Struct = txn.readRow("orders", Key.of(productOrders.orderID), Arrays.asList("state"))
      logger.debug(s"state of order ${productOrders.orderID} is $stateOrder")

      // check if order is already validated
      if (!stateOrder.getString(0).equals("VALIDATED")) {
        logger.debug("*** order is not validated so keep inserting in it ***")

        // check if a productOrder record already contains a specific productID & orderID
        val existingPO = Option(txn.readRow("product_orders", Key.of(productOrders.productID, productOrders.orderID), columns))
        /*logger.debug(s"${existingPO}")*/

        existingPO match {
          case Some(po: Struct) => {
            // if a such record exists, only update quantity

            val existingQuantity = po.getLong("quantity")
            val updateQuantity = existingQuantity + productOrders.quantity

            val stockByProductID = txn.readRowUsingIndex("stock", "StockByProductID", Key.of(productOrders.productID), Arrays.asList("product_id", "stock", "id"))
            val stockID = stockByProductID.getString("id")
            val currentStock = stockByProductID.getLong("stock")
            val updatedStock = currentStock - productOrders.quantity

            val updateQuantityMutation = Mutation.newUpdateBuilder(table)
                    .set("order_id").to(productOrders.orderID)
                    .set("product_id").to(productOrders.productID)
                    .set("quantity").to(updateQuantity)
                    .build

            val updateStockMutation = Mutation.newUpdateBuilder("stock")
              .set("id").to(stockID)
              .set("stock").to(updatedStock)
              .build()


            val update2Do: Collection[Mutation] = Arrays.asList(updateQuantityMutation, updateStockMutation)


            txn.buffer(update2Do)

          }
          case None => {

            // if nothing exists, insert a new ProductOrder into database + select stocks correspondant +  update stock


            val stockByProductID = txn.readRowUsingIndex("stock", "StockByProductID", Key.of(productOrders.productID), Arrays.asList("product_id", "stock", "id"))

            val stockID = stockByProductID.getString("id")

            val currentStock = stockByProductID.getLong("stock")

            val updatedStock = currentStock - productOrders.quantity

            val insertProductOrderMutation = Mutation.newInsertBuilder(table)
                    .set("product_id").to(productOrders.productID)
                    .set("order_id").to(productOrders.orderID)
                    .set("quantity").to(productOrders.quantity)
                    .build()


            val updateStockMutation = Mutation.newUpdateBuilder("stock")
                    .set("id").to(stockID)
                    .set("stock").to(updatedStock)
                    .build()


            val update2Do: Collection[Mutation] = Arrays.asList(insertProductOrderMutation, updateStockMutation)


            txn.buffer(update2Do)

          }
        }
      } else {
        logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | Error Type : OrderAlreadyValidated")
      }

      try {

        manager.commit()
        val state = manager.getState.toString
        if (state != "COMMIT_FAILED" || state != "ROLLED_BACK") 1 else 0

      } catch {
        case e: AbortedException =>
          Thread.sleep(e.getRetryDelayInMillis / 1000)
          txn = manager.resetForRetry
          logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | Error Type : RETRY ${e.getReason}")
          runTransaction(productOrders, nbRetry + 1)


        case se: SpannerException =>
          logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | Error Type : SPANNEREXCEPTION ${se.getErrorCode()}")
          0

        case unknown: Throwable =>
          logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | Error Type : UNKNOWN ${unknown.getStackTrace}")
          0
      }
    }

    logger.debug(s"Excecute insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")


    runTransaction(productOrders, 0) match {
      case 1 => {
        if (manager != null) manager.close()
        logger.debug(s"Success insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
      }
      case 0 => {
        if (manager != null) manager.close()
        logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | Error Type : RetryTooOld")
        0
      }
    }
  }

  override def delete(OrderID: String, productID: String, quantity: Int): Int = ???


  override def truncateProductOrder(): Int = {
    MDC.put("method", "truncateProductOrder")

    val mutations = new ArrayList[Mutation]
    mutations.add(Mutation.delete(table, KeySet.all))

    try {
      connection.write(mutations) match {
        case ts =>
          logger.debug(s"ProductOrders deleted with success at ${ts.toDate}")
          1
      }
    } catch {
      case _: SpannerException =>
        logger.debug("Delete failed....")
        0
    }

  }

  override def getNumberOfproductOrders(): Int = {
    MDC.put("method", "getNumberOfproductOrders")

    logger.debug(s"Execute getNumberOfproductOrders() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

    val getNumberOfproductOrders: Statement = Statement.of("SELECT sum(quantity) FROM product_orders")
    val rs: ResultSet = connection.singleUse().executeQuery(getNumberOfproductOrders)


    val numberProductOrders: Int = if (rs.next) {
      val res: Int = rs.getLong(0).toInt
      logger.info(s"Execute getNumberOfproductOrders() | NumberProductOrders : $res ")
      res
    } else 0
    numberProductOrders
  }
}
