package dao.postgres

import java.sql.{Connection, PreparedStatement, ResultSet}

import dao.AAvailableProductRepository
import models.AvailableProduct
import org.slf4j.MDC
import play.api.Logger


class PostgresAvailableProductRepository(connection: Connection) extends AAvailableProductRepository {

  val logger: Logger = Logger(this.getClass())

  val qGetProductWithOffset: String = "SELECT id, name, category, brand, description, uri, price FROM product OFFSET ? LIMIT ? ;"

  val preparedGetProduct: PreparedStatement = connection.prepareStatement(qGetProductWithOffset)

  override def getProductsWithOffset(offset: Int, limit: Int): Option[List[AvailableProduct]] = {
    MDC.put("method", "getProductsWithOffset")

    def resultSetToProduct(resultSet: ResultSet, listAttributes: List[AvailableProduct] = Nil): List[AvailableProduct] = {

      if(resultSet.next()) {

        val id: String = resultSet.getObject("id").toString
        val name: String = resultSet.getObject("name").toString
        val category: String = resultSet.getObject("category").toString
        val brand: String = resultSet.getObject("brand").toString
        val description: String = resultSet.getObject("description").toString
        val uri: String = resultSet.getObject("uri").toString
        val price: Double = resultSet.getObject("price").toString.toDouble
        val stock: Int = 500
        val location: String = "France"
        val nextDelivery: String = "2018-08-20 10:04:43.334968+00:00"
        val avProduct = AvailableProduct(id, name, category, brand, description, uri, price, stock, location, nextDelivery)

        resultSetToProduct(resultSet, avProduct :: listAttributes)
      }

      else listAttributes

    }
      preparedGetProduct.clearParameters()
      preparedGetProduct.setInt(1, offset)
      preparedGetProduct.setInt(2, limit)

      logger.debug(s"Excecute getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")

      val resultSet = preparedGetProduct.executeQuery()

      resultSetToProduct(resultSet) match {
        case products: List[AvailableProduct] =>
          logger.debug(s"Success getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
          Some(products)
        case Nil =>
          logger.error(s"Failed getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
          None
      }
  }
}
