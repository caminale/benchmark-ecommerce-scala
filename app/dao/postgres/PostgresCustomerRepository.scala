package dao.postgres

import java.sql.{Connection, PreparedStatement, ResultSet}

import dao.ACustomerRepository
import models.Customer
import org.slf4j.MDC
import play.api.Logger

class PostgresCustomerRepository(connection: Connection) extends ACustomerRepository {
  val logger: Logger = Logger(this.getClass())

  val qGetAllCustomers: String = "SELECT * FROM customer;"
  var preparedGetAllCustomers: PreparedStatement = connection.prepareStatement(qGetAllCustomers)

  override def getAllCustomers(): Option[List[Customer]] =  {

    MDC.put("method", "getAllCustomers")

    @scala.annotation.tailrec
    def resultSetToCustomer(resultSet: ResultSet, listAttributes: List[Customer] = Nil): List[Customer] = {
      if(resultSet.next()) {
        val id: String = resultSet.getObject("id").toString
        val name: String = resultSet.getObject("name").toString
        val email: String =  resultSet.getObject("email").toString
        val customer: Customer =  Customer(id, name, email)
        resultSetToCustomer(resultSet, customer :: listAttributes)
      }
      else listAttributes
    }
      logger.debug(s"Excecute getAllCustomers(), Connection : $connection ThreadID : ${Thread.currentThread().getName}")

      val resultSet: ResultSet = preparedGetAllCustomers.executeQuery()
      resultSetToCustomer(resultSet) match {
        case customers: List[Customer] =>
          Some(customers)
        case Nil =>
          None
      }
  }
}
