package dao.fauna



import dao.AProductRepository
import dao.fauna.FaunaFactory.await
import faunadb.values.Value
import faunadb.{FaunaClient, query => q, values => v}
import models.Product
import org.slf4j.MDC
import play.api.Logger



class FaunaProductRepository (connection: FaunaClient) extends AProductRepository with ContextExecution {

  val logger: Logger = Logger(this.getClass())
  implicit val productCodec = v.Codec.caseClass[Product]

  override def getProductByProductId(id: String): Option[Product] = {
    //TODO add Try + match
    MDC.put("method", "getProductByProductId")
    logger.debug(s"Excecute getProductByProductId(id: $id) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
  
  
    val res = connection.query(
      q.If(
        q.Exists(
          q.Match(q.Index("product_by_id"), id)
        ),
        q.Let {
          val productRecord = q.Get(q.Match(q.Index("product_by_id"), id))
          q.Select(q.Arr(Value("data")), productRecord)
        },
        s"productID $id doesn't exist.."
      )
    )
    
    await(res) match {
      case resStringError: faunadb.values.StringV =>
        logger.error(s"Failed getProductByProductId(id: $id) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | error: ${resStringError.toString}")
        None
      case resCustomer: faunadb.values.ObjectV =>
        logger.debug(s"Success getProductByProductId(id: $id) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        Option(resCustomer.to[Product].get)
    }
  }

  override def getProductByListProductID(productsID: List[String]): List[Option[Product]] = {
    MDC.put("method", "getProductByListProductID")
  
    logger.debug(s"Excecute getProductByListProductID(productsID: $productsID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    productsID.map {
      element => getProductByProductId(element)
    }
  }
}
