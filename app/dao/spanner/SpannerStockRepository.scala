package dao.spanner

import com.google.cloud.{Timestamp => gTS}
import java.sql.Timestamp
import java.util.UUID.randomUUID
import java.util.{Arrays, Collections}
import scala.collection.JavaConverters._
import com.google.cloud.spanner._
import com.typesafe.config.ConfigFactory
import dao.AStockRepository
import models.{Product, Stock}
import org.slf4j.MDC
import play.api.Logger
import scala.collection.JavaConverters._


class SpannerStockRepository(connection: DatabaseClient = null) extends AStockRepository {
  
  //TODO refacto connection for PopulateDB
  
  val logger: Logger = Logger(this.getClass)
  
  val table = "stock"
  val columns = Arrays.asList("id", "product_id", "stock", "location", "last_delivery", "next_delivery")
  
  
  def insert(stock: Stock, client: DatabaseClient): Int = {
    MDC.put("method", "insert")
    
    val id = stock.id
    val productID = stock.productID
    val stocks = stock.stock
    val location = stock.location
    val lastDelivery = stock.lastDelivery
    val nextDelivery = stock.nextDelivery
    
    
    val mutation = Mutation.newInsertBuilder(table)
            .set(columns.get(0)).to(id)
            .set(columns.get(1)).to(productID)
            .set(columns.get(2)).to(stocks)
            .set(columns.get(3)).to(location)
            .set(columns.get(4)).to(lastDelivery)
            .set(columns.get(5)).to(nextDelivery)
            .build
    
    try {
      client.write(Collections.singletonList(mutation))
      logger.trace(s"Stock $id for product $productID has been inserted");
      1
    } catch {
      case _: SpannerException => logger.trace(s"An error occurred with stock $id to be inserted"); 0
    }
  }
  
  
  def populateStockWithProductList(productList: List[Product], divisor: Int, category: String, client: DatabaseClient): List[Int] = {
    MDC.put("method", "populateStockWithProductList")
    
    
    def stocksToMutationList(sliceList: List[Product]): List[Mutation] = {
      
      val r = scala.util.Random
      val locations: List[String] = List("France", "Italia", "Germany", "Brasilia")
      val dayInMs: Long = 86400000
      
      sliceList.map {
        product =>
          val id = randomUUID().toString
          val productID = product.id.toString
          val stock = ConfigFactory.load().getInt("api.stockQuantity")
          val location = locations(r.nextInt(locations.length))
          val lastDelivery = new Timestamp(System.currentTimeMillis()).toString
          val nextDelivery = new Timestamp(System.currentTimeMillis() + 30 * dayInMs).toString
          
          Mutation.newInsertBuilder(table)
                  .set(columns.get(0)).to(id)
                  .set(columns.get(1)).to(productID)
                  .set(columns.get(2)).to(stock)
                  .set(columns.get(3)).to(location)
                  .set(columns.get(4)).to(lastDelivery)
                  .set(columns.get(5)).to(nextDelivery)
                  .build
      }
    }
    
    val listLength = productList.length
    val iterator: Int = listLength / divisor
    val listIterator: List[Int] = List.range(0, divisor)
    
    try {
      listIterator.map(
        i =>
          client.write(stocksToMutationList(productList.slice(iterator * i, iterator * (i + 1))).asJava) match {
            case ts: gTS => logger.debug(s"${i + 1}/${divisor} $category stocks has been inserted at ${ts.toDate}");
              1
          })
    } catch {
      case e: SpannerException => logger.debug(s"An error occured and customer list can not be inserted : ${e.getErrorCode}");
        List.fill(listIterator.length)(0)
    }
  }
  
  override def updateAllStockQuantity(): Int = {
    MDC.put("method", "updateAllStockQuantity")
    
    logger.debug(s"Excecute updateAllStockQuantity(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")
    
    val stocksID = getAllStocksID(connection)
    val intList = updateStock(stocksID, divisor = 100, connection)
    if (intList.contains(0)) {
      logger.debug("update stock failed...")
      0
    }
    else {
      logger.debug("update stock success")
      1
    }
    
  }
  
  
  def getAllStocksID(connection: DatabaseClient): List[String] = {
    MDC.put("method", "getAllStocksID")
    
    @scala.annotation.tailrec
    def getResultStock(resultSet: ResultSet, list: List[String] = Nil): List[String] = {
      if (resultSet.next()) {
        val id = resultSet.getString("id")
        getResultStock(resultSet, id :: list)
      }
      else {
        list
      }
    }
    
    
    val sqlStatement = Statement.of("SELECT id FROM stock")
    
    logger.debug(s"Excecute getAllStocksID(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")
    
    val resultSet: ResultSet = connection.singleUse().executeQuery(sqlStatement)
    val stocks: List[String] = getResultStock(resultSet)
    
    try {
      stocks match {
        case pStocks =>
          logger.debug(s"Success getAllStocksID(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")
          pStocks
      }
    } catch {
      
      case _: SpannerException =>
        logger.error(s"Failed getAllStocksID(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")
        List.empty[String]
    }
  }
  
  
  def updateStock(stockIDList: List[String], divisor: Int, connection: DatabaseClient): List[Int] = {
    MDC.put("method", "updateStock")
    
    val stockValue = ConfigFactory.load().getInt("api.stockQuantity")
    
    def stockIDToMutationList(sliceList: List[String]): List[Mutation] = {
      sliceList.map {
        id =>
          Mutation.newUpdateBuilder("stock")
                  .set("id").to(id)
                  .set("stock").to(stockValue)
                  .build()
        
      }
    }
    
    val listLength = stockIDList.length
    val iterator: Int = listLength / divisor
    val listIterator: List[Int] = List.range(0, divisor)
    
    try {
      listIterator.map {
        i =>
          connection.write(stockIDToMutationList(stockIDList.slice(iterator * i, iterator * (i + 1))).asJava)
          1
      }
    } catch {
      case _: SpannerException => List.fill(listIterator.length)(0)
    }
  }
}
