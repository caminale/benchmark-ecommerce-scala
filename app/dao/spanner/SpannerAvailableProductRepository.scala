package dao.spanner

import com.google.cloud.spanner.{DatabaseClient, ResultSet, Statement}
import dao.AAvailableProductRepository
import models.{AvailableProduct, Product}
import org.slf4j.MDC
import play.api.Logger


class SpannerAvailableProductRepository(connection: DatabaseClient) extends AAvailableProductRepository {
  val logger: Logger = Logger(this.getClass())
  
  override def getProductsWithOffset(offset: Int, limit: Int): Option[List[AvailableProduct]] = {
    MDC.put("method", "getProductsWithOffset")
    
    
    @scala.annotation.tailrec
    def getResultProduct(resultSet: ResultSet, list: List[Product] = Nil): List[Product] = {
      if (resultSet.next()) {
        val id = resultSet.getString(0)
        val name = resultSet.getString(1)
        val category = resultSet.getString(2)
        val brand = resultSet.getString(3)
        val description = resultSet.getString(4)
        val uri = resultSet.getString(5)
        val price = resultSet.getDouble(6)
        val product = Product(id, name, category, brand, description, uri, price, true)
        getResultProduct(resultSet, product :: list)
      }
      else {
        list
      }
    }
    
    def join(productList: List[Product], resultSetStock: ResultSet, list: List[AvailableProduct] = Nil, index: Int = 0): List[AvailableProduct] = {
      if (resultSetStock.next) {
        val element = productList(index)
        val id = element.id
        val name = element.name
        val category = element.category
        val brand = element.brand
        val description = element.description
        val uri = element.uri
        val price = element.price
        val stock = resultSetStock.getLong(2).toInt
        val location = resultSetStock.getString(3)
        val nextDelivery = resultSetStock.getString(5)
        val av = AvailableProduct(id, name, category, brand, description, uri, price, stock, location, nextDelivery)
        
        join(productList, resultSetStock, av :: list, index + 1)
      }
      else {
        list
      }
    }
    
    val sqlProductStatement = Statement.of(s"SELECT id, name, category, brand, description, uri, price FROM product where visible = true ORDER BY id ASC LIMIT ${limit + 1} OFFSET ${offset}")
    
    logger.debug(s"Excecute getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    
    val productResultSet: ResultSet = connection.singleUse().executeQuery(sqlProductStatement)
    val productList = getResultProduct(productResultSet)
    
    val productIDListTemp = productList.map(_.id)
    val productIDList = productIDListTemp.mkString("'", "','", "'")
    
    
    // TODO, pour >=30k produits :  "INVALID_ARGUMENT: Query string length of 1170186 exceeds maximum allowed length of 1048576" - faire plusieurs requêtes au lieu d'une
    val sqlStockStatement = Statement.of(s"SELECT s.id, s.product_id, s.stock, s.location, s.last_delivery, s.next_delivery FROM stock as s WHERE s.product_id IN (${productIDList}) ORDER BY s.product_id DESC")
    
    val stockResultSet = connection.singleUse().executeQuery(sqlStockStatement)
    
    val avList: List[AvailableProduct] = join(productList, stockResultSet)
    
    avList match {
      case products: List[AvailableProduct] =>
        logger.debug(s"Success getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        Some(products)
      
      case Nil =>
        logger.error(s"Failed getProductsWithOffset(offset: $offset, limit: $limit) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} + ${avList.length}")
        None
      
      
    }
  }
}