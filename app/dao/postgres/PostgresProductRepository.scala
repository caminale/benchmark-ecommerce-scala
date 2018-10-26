package dao.postgres

import java.sql.{Connection, PreparedStatement, ResultSet}

import dao.AProductRepository
import models.Product
import org.slf4j.MDC
import play.api.Logger

class PostgresProductRepository(connection: Connection) extends AProductRepository {

  val logger: Logger = Logger(this.getClass())

  val qGetProductByID: String = "SELECT * FROM product WHERE id = ? ;"
  var preparedGetProductByID: PreparedStatement = connection.prepareStatement(qGetProductByID)

  override def getProductByProductId(id: String): Option[Product] = {
    MDC.put("method", "getProductByProductId")

    @scala.annotation.tailrec
    def resultSetToProduct(resultSet: ResultSet, listAttributes: List[Product] = Nil): List[Product] = {

      if(resultSet.next()) {
        val id: String = resultSet.getObject("id").toString
        val name: String = resultSet.getObject("name").toString
        val category: String = resultSet.getObject("category").toString
        val brand: String = resultSet.getObject("brand").toString
        val description: String = resultSet.getObject("description").toString
        val uri: String = resultSet.getObject("uri").toString
        val price: Double = resultSet.getObject("price").toString.toDouble
        val visible: Boolean = resultSet.getObject("visible").toString.toBoolean
        val product = new Product(id, name, category, brand, description, uri, price, visible)
        resultSetToProduct(resultSet, product :: listAttributes)
      }
      else listAttributes
    }

    preparedGetProductByID.setObject(1, java.util.UUID.fromString(id))

    logger.debug(s"Excecute getProductByProductId(id: $id) | Connection :  ${connection} | " +
      s"ThreadID : ${Thread.currentThread().getName}")

    val resultSet = preparedGetProductByID.executeQuery()
   resultSetToProduct(resultSet) match {
      case products: List[Product] =>
        logger.debug(s"Success getProductByProductId(id: $id) | Connection :  ${connection} | " +
          s"ThreadID : ${Thread.currentThread().getName}")
        Some(products(0))
      case Nil =>
        logger.error(s"Failed getProductByProductId(id: $id) | Connection :  ${connection} | " +
          s"ThreadID : ${Thread.currentThread().getName} | Error Type: No Product found")
        None
    }
  }

  def getProductByListProductID(productsID: List[String]): List[Option[Product]]= {
    MDC.put("method", "getProductByListProductID")
    logger.debug(s"Excecute getProductByListProductID(productsID: $productsID) | " +
      s"Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    productsID.map(productId => getProductByProductId(productId))
  }

}
