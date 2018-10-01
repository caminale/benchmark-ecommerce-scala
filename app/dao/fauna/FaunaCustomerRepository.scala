package dao.fauna

import dao.ACustomerRepository
import dao.fauna.FaunaFactory.await
import faunadb.values.Value
import faunadb.{FaunaClient, query => q, values => v}
import models.Customer
import play.api.Logger

import scala.util.{Success, Try, Failure}

class FaunaCustomerRepository(connection: FaunaClient) extends ACustomerRepository with ContextExecution{

  val logger: Logger = Logger(this.getClass())
  implicit val customerCodec = v.Codec.caseClass[Customer]

  override def getAllCustomers(): Option[List[Customer]] = {
  
    val pageSize: Int = 5
    var cursorPos: Option[v.Value] = None
    var res: List[Value] = null

    logger.debug(s"Excecute getAllCustomers(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")

    do {
      val result = Try {
        val stmt = connection.query(
          q.Map(
            q.Paginate(q.Match(q.Index("customer_id_filter")),
              cursor = cursorPos.map(q.After(_)) getOrElse q.NoCursor,
              size = pageSize),
            q.Lambda { x => q.Select("data", q.Get(q.Select(1, x))) }
          )
        )
        await(stmt)
      }
      res = result match {
        case Success(s) =>
          logger.debug(s"Success getAllCustomers(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")
          val data = s("data").to[List[v.Value]].get
          cursorPos = s("after").toOpt
          data

        case Failure(e) =>
          logger.error(s"Failed getAllCustomers(), Connection : $connection ThreadID : ${Thread.currentThread().getName} | error : $e")
          null
      }
    } while (cursorPos.isDefined)
    Option(res.map(x => x.to[Customer].get))
  }

}
