package dao.fauna

import dao.AProductOrderRepository
import dao.fauna.FaunaFactory._
import faunadb.{FaunaClient, query => q, values => v}
import models.ProductOrder
import org.slf4j.MDC
import play.api.Logger

import scala.util.{Failure, Success, Try}

class FaunaProductOrderRepository (connection: FaunaClient) extends AProductOrderRepository with ContextExecution {

  val logger: Logger = Logger(this.getClass())

  implicit val productOrderCodec = v.Codec.caseClass[ProductOrder]

  override def insert(orderID: String, productID: String, quantity: Int): Int = {
    MDC.put("method", "insertProductOrder")

    logger.debug(s"Excecute insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

    val res = Try {

      val stmt = connection.query(
        q.Let {
          val orderRecord = q.Get(q.Match(q.Index("orders_by_id"), orderID))
          q.If(
            q.Equals(q.Select(q.Arr("data", "state"), orderRecord), "PROCESS"),
            q.Do(
              q.Create(
                q.Class("product_orders"),
                q.Obj("data" -> ProductOrder(java.util.UUID.randomUUID().toString, productID, orderID, quantity))),
              q.Map(
                q.Arr(q.Get(q.Match(q.Index("stock_by_product_id"), productID))),
                q.Lambda {
                  stock =>
                    val stockRef = q.Select("ref", stock)
                    val stockValue = q.Select(q.Arr("data", "stock"), stock)
                    q.Update(
                      stockRef,
                      q.Obj("data" -> q.Obj("stock" -> q.Subtract(stockValue, quantity))))
                }
              )
            ),
            "ORDER ALREADY VALIDATED"
          )
        }
      )
      await(stmt)
    }
    res match {
      case Success(resStringError: faunadb.values.StringV) =>
        logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | $resStringError")
        0
      case Success(_: faunadb.values.Value) =>
        logger.debug(s"Success insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
      case Failure(e) =>
        logger.error(s"Failed insertProductOrder(orderID: $orderID, productID: $productID, quantity: $quantity) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | error : $e")
        0
    }
  }
  //TODO faire truncate
  override def truncateProductOrder(): Int = ???
  override def delete(OrderID: String, productID: String, quantity: Int): Int = ???
  override def getNumberOfproductOrders(): Int = ???

}

