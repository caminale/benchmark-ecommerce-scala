package dao.fauna

import dao.AAvailableProductRepository
import dao.fauna.FaunaFactory.await
import faunadb.values.Value
import faunadb.{FaunaClient, query => q, values => v}
import models.AvailableProduct
import org.slf4j.MDC
import play.api.Logger

import scala.util.{Success, Try, Failure}


class FaunaAvailableProductRepository(connection: FaunaClient) extends AAvailableProductRepository with ContextExecution {

  val logger: Logger = Logger(this.getClass())
  implicit val productCodec = v.Codec.caseClass[AvailableProduct]

  override def getProductsWithOffset(offset: Int, limit: Int): Option[List[AvailableProduct]] = {
    MDC.put("method", "getProductsWithOffset")

    var cursorPos: Option[v.Value] = None
    var res: List[Value] = null
    val offsetNbrProduct: Int = 2
    val nbrTotal: Int = 16
    val pageSize: Int = nbrTotal
    logger.debug(s"Excecute getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

    do {
      val result = Try {
        val stmt = connection.query(
          q.Map(
            q.Paginate(q.Match(q.Index("product_id_filter")),
              cursor = cursorPos.map(q.After(_)) getOrElse q.NoCursor,
              size = pageSize),
            q.Lambda { x =>
              val productID = q.Select(q.Arr("data", "id"), q.Get(q.Select(1, x)))
              val productRecord = q.Get(q.Select(1, x))
              val stockRecord = q.Get(q.Match(q.Index("stock_by_product_id"), productID))
              q.Obj(
                "stock" -> q.Select(q.Arr("data", "stock"), stockRecord),
                "location" -> q.Select(q.Arr("data", "location"), stockRecord),
                "nextDelivery" -> q.Select(q.Arr("data", "nextDelivery"), stockRecord),
                "id" -> q.Select(q.Arr("data", "id"), productRecord),
                "name" -> q.Select(q.Arr("data", "name"), productRecord),
                "category" -> q.Select(q.Arr("data", "category"), productRecord),
                "brand" -> q.Select(q.Arr("data", "brand"), productRecord),
                "description" -> q.Select(q.Arr("data", "description"), productRecord),
                "uri" -> q.Select(q.Arr("data", "uri"), productRecord),
                "price" -> q.Select(q.Arr("data", "price"), productRecord)
              )
            }
          )
        )
        await(stmt)
      }
      res = result match {
        case Success(res: v.Value) => {
          val data: List[Value] = res("data").to[List[v.Value]].get
          logger.debug(s"Success getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
          cursorPos = res("after").toOpt
          data
        }
        case Failure(e) =>
          logger.error(s"Failed getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | error : $e")
          null
      }
    } while (cursorPos.isDefined)
    /*.slice(offsetNbrProduct - 1,nbrTotal+offsetNbrProduct)*/

    Option(res.map(x => x.to[AvailableProduct].get))
  }
}