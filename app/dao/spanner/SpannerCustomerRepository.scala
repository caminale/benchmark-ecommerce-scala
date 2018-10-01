package dao.spanner

import com.google.cloud.spanner._
import java.util.{Arrays, Collections}
import com.google.cloud.Timestamp

import dao.ACustomerRepository
import models.Customer
import play.api.Logger
import org.slf4j.MDC

import scala.collection.JavaConverters._

class SpannerCustomerRepository(connection: DatabaseClient = null) extends ACustomerRepository {
  
  
  val logger: Logger = Logger(this.getClass)
  
  private val table = "customer"
  private val columns = Arrays.asList("id", "name", "email")
  
  
  override def getAllCustomers(): Option[List[Customer]] = {
    MDC.put("method", "getAllCustomers")
    
    @scala.annotation.tailrec
    def getResultCustomer(resultSet: ResultSet, list: List[Customer] = Nil): List[Customer] = {
      if (resultSet.next()) {
        val id = resultSet.getString("id")
        val name = resultSet.getString("name")
        val email = resultSet.getString("email")
        val customer = Customer(id, name, email)
        getResultCustomer(resultSet, customer :: list)
      }
      else {
        list
      }
    }
    
    
    val sqlStatement = Statement.of("SELECT id, name, email FROM customer")
    
    logger.debug(s"Excecute getAllCustomers(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")
    
    val resultSet: ResultSet = connection.singleUse().executeQuery(sqlStatement)
    val customers: List[Customer] = getResultCustomer(resultSet)
    
    customers match {
      case pCustomers: List[Customer] =>
        logger.debug(s"Success getAllCustomers(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")
        Some(pCustomers)
      
      case Nil =>
        logger.error(s"Failed getAllCustomers(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")
        None
      
      
    }
  }
  
  
  def populateCustomerWithList(customerList: List[Customer], divisor: Int, client: DatabaseClient): List[Int] = {
    MDC.put("method", "populateCustomerWithList")
    
    
    def customersToMutationList(sliceList: List[Customer]): List[Mutation] = {
      sliceList.map {
        customer =>
          val id = customer.id
          val name = customer.name
          val email = customer.email
          
          Mutation.newInsertBuilder(table)
                  .set(columns.get(0)).to(id)
                  .set(columns.get(1)).to(name)
                  .set(columns.get(2)).to(email)
                  .build
      }
    }
    
    val listLength = customerList.length
    val iterator: Int = listLength / divisor
    val listIterator: List[Int] = List.range(0, divisor)
    
    try {
      listIterator.map(i => client.write(customersToMutationList(customerList.slice(iterator * i, iterator * (i + 1))).asJava) match {
        case ts: Timestamp => logger.debug(s"${i + 1}/$divisor of Customer list has been inserted at ${ts.toDate}"); 1
      })
    }
    catch {
      case e: SpannerException => logger.debug(s"An error occured and customer list can not be inserted.... + ${e.getMessage}");
        List.empty[Int]
    }
  }
}
