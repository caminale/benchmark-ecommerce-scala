package dao.fauna

import java.sql.Timestamp

import dao.AOrderRepository
import dao.fauna.FaunaFactory.await
import faunadb.query.Expr
import faunadb.{FaunaClient, query => q, values => v}
import models.Order
import org.slf4j.MDC
import play.api.Logger

import scala.util.{Failure, Success, Try}

class FaunaOrderRepository(connection: FaunaClient) extends AOrderRepository with ContextExecution {

  val logger: Logger = Logger(this.getClass())
  implicit val productCodec = v.Codec.caseClass[Order]

  val className: String = "orders"
  val orderByID = "orders_by_id"



  override def insert(orderID: String, customerID: String): Int = {
    MDC.put("method", "insert")
    logger.debug(s"Excecute insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    val currentTimestamp = new Timestamp(System.currentTimeMillis()).toString
    val state = "PROCESS"
    val order = Order(orderID, customerID, currentTimestamp, state)

    val result = Try {
      val stmt = connection.query(
        q.Create(
          q.Class(className),
          q.Obj("data" -> order)
        )
      )
      await(stmt)
    }
    result match {
      case Success(_) =>
        logger.debug(s"Success insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
      case Failure(e) =>
        logger.error(s"Failed insert(orderID: $orderID, customerID: $customerID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }

  override def validationOrder(orderID: String): Int = {
    //TODO mettre le Try + match
    MDC.put("method", "validationOrder")
    logger.debug(s"Execute Validation Order: $orderID | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

    val res = connection.query(
      q.If(
        q.Exists(
          q.Match(q.Index(orderByID), orderID)
        ),
        {
          val ref: Expr = q.Select("ref", q.Get(q.Match(q.Index(orderByID), orderID)))

          q.Update(ref,
            q.Obj("data" -> q.Obj(
              "state" -> "VALIDATED"
            )
            )
          )
        },
        s"$orderID is not in the index"
      )

    ).map {
      case resStringError: faunadb.values.StringV  =>
        logger.error(s"Failed Validation Order: $orderID | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | error : ${resStringError.toString}")
        0
      case _ : faunadb.values.ObjectV =>
        logger.debug(s"Success Validation Order: $orderID | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
    }

    await(res)


  }

  override def truncateOrder(): Int = {
    MDC.put("method", "truncateOrder")
    val adminClient = FaunaFactory.adminGetConnection()

    logger.debug(s"Execute truncateOrder | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    Try {
      val truncate = adminClient.query(
        q.Arr(
          q.If(
            q.Exists(q.Class(className)),
            q.Delete(q.Class(className)),
            true
          ),
          q.CreateClass(q.Obj("name" -> className))
        )
      )
      await(truncate)
    } match {
      case Success(_) =>
        logger.debug(s"Success truncateOrder | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}"); 1
      case Failure(e) =>
        logger.error(s"Failed truncateOrder | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | error: $e" ); 0
    }
  }
  override def getNumberValidatedOrder(): Int = ???
}
