package dao.spanner


import java.util.{ArrayList, Arrays, Collections}

import com.google.cloud.spanner._
import dao.AProductRepository
import models.Product

import scala.collection.JavaConverters._
import scala.concurrent.Future


// models

// Logs + Metrics
import org.slf4j.MDC
import play.api.Logger


class SpannerProductRepository(connection: DatabaseClient = null) extends AProductRepository {
  
  val logger: Logger = Logger(this.getClass)
  
  
  val table = "product"
  val columns = Arrays.asList("id", "name", "brand", "category", "description", "uri", "price", "visible")
  
  
  def mappingProduct(struct: Struct): Product = {
    Product(struct.getString(0), struct.getString(1), struct.getString(2),
      struct.getString(3), struct.getString(4), struct.getString(5),
      struct.getDouble(6), struct.getBoolean(7))
  }
  
  override def getProductByProductId(id: String): Option[Product] = {
    MDC.put("method", "getProductByProductId")
    
    val txn = connection.singleUseReadOnlyTransaction()
    
    
    logger.debug(s"Excecute getProductByProductId(id: $id) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    
    val struct: Option[Struct] = Option(txn.readRow(table, Key.of(id), columns))
    
    struct match {
      
      case Some(struct) =>
        logger.debug(s"Success getProductByProductId(id: $id) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        Some(mappingProduct(struct))
      
      case None =>
        logger.error(s"Failed getProductByProductId(id: $id) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName} | Error Type: No Product found")
        None
    }
  }
  
  override def getProductByListProductID(productsID: List[String]): List[Option[Product]] = {
    MDC.put("method", "getProductByListProductID")
    logger.debug(s"Excecute getProductByListProductID(productsID: $productsID) | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    
    productsID.map {
      id => getProductByProductId(id)
    }
  }
  
  def populateProductWithList(productList: List[Product], divisor: Int, category: String, client: DatabaseClient): List[Int] = {
    MDC.put("method", "populateProductWithList")
    
    def productsToMutationList(sliceList: List[Product]): List[Mutation] = {
      sliceList.map {
        product =>
          val id = product.id
          val name = product.name
          val brand = product.brand
          val category = product.category
          val description = product.description
          val uri = product.uri
          val price = product.price
          val visible = product.visible
          
          
          Mutation.newInsertBuilder(table)
                  .set(columns.get(0)).to(id)
                  .set(columns.get(1)).to(name)
                  .set(columns.get(2)).to(brand)
                  .set(columns.get(3)).to(category)
                  .set(columns.get(4)).to(description)
                  .set(columns.get(5)).to(uri)
                  .set(columns.get(6)).to(price)
                  .set(columns.get(7)).to(visible)
                  .build
      }
    }
    
    
    val listLength = productList.length
    val iterator: Int = listLength / divisor
    val listIterator: List[Int] = List.range(0, divisor)
    
    try {
      listIterator.map(
        i =>
          client.write(productsToMutationList(productList.slice(iterator * i, iterator * (i + 1))).asJava) match {
            case ts => logger.debug(s"${i + 1}/$divisor of $category product has been inserted at ${ts.toDate}")
              1
          })
    } catch {
      case _: SpannerException => logger.debug(s"An error occured and customer list can not be inserted....")
        List.fill(listIterator.length)(0)
    }
  }
  
  
  def getVisibleProductWithOffset(offset: Int, limit: Int, client: DatabaseClient): Option[List[Product]] = {
    
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
        val visible = resultSet.getBoolean(7)
        val product = Product(id, name, category, brand, description, uri, price, visible)
        getResultProduct(resultSet, product :: list)
      }
      else {
        list
      }
    }
    
    
    val sqlStatement = Statement.of(s"SELECT id, name, category, brand, description, uri, price, visible FROM product WHERE visible = true LIMIT $limit OFFSET $offset")
    
    val resultSet: ResultSet = connection.singleUse().executeQuery(sqlStatement)
    
    val products: List[Product] = getResultProduct(resultSet)
    
    products match {
      case p: List[Product] => Some(p)
      case Nil => None
    }
  }
  
  
}
