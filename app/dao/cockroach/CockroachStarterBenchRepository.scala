package dao.cockroach

import java.sql.{Connection, PreparedStatement, ResultSet}

import dao.AStarterBenchRepository
import org.slf4j.MDC
import play.api.Logger

class CockroachStarterBenchRepository(connection: Connection) extends AStarterBenchRepository {
  val logger: Logger = Logger(this.getClass)

  val qInsert: String = "UPSERT INTO bench(id, is_ready) VALUES(?, ?);"
  val qGetIsReady: String = "SELECT is_ready FROM bench WHERE id = 'starter';"
  val qTruncate: String = "USE octo; TRUNCATE bench CASCADE;"

  val preparedInsert : PreparedStatement = connection.prepareStatement(qInsert)
  val preparedGetIsReady: PreparedStatement = connection.prepareStatement(qGetIsReady)
  val preparedTruncate: PreparedStatement = connection.prepareStatement(qTruncate)
  
  
  override def insertStarterBench(id: String, b: Boolean): Int = {
    MDC.put("method", "insertStarterBench")

    preparedInsert.clearParameters()
    preparedInsert.setString(1,  id)
    preparedInsert.setBoolean(2, b)
    logger.debug(s"Excecute insertStarterBench() | Connection :  ${connection} | " +
      s"ThreadID : ${Thread.currentThread().getName}")

    preparedInsert.executeUpdate() match {
      case 1 =>
        logger.debug(s"Success insertStarterBench() | Connection :  ${connection} | " +
          s"ThreadID : ${Thread.currentThread().getName}")
        1
      case 0 =>
        logger.error(s"Failed insertStarterBench() | Connection :  ${connection} | " +
          s"ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }

  override def getStarterBench(): Boolean = {
    MDC.put("method", "getStarterBench")

    logger.debug(s"Execute getStarterBench() | Connection :  ${connection} |" +
      s" ThreadID : ${Thread.currentThread().getName}")

    val rsIsReady: ResultSet = preparedGetIsReady.executeQuery()

    val isReady: Boolean = if(rsIsReady.next) {
      val res: Boolean = rsIsReady.getObject("is_ready").toString.toBoolean
      res
    } else false

    isReady
  }
  
  override def truncateStarterBench(): Int = {
    MDC.put("method", "truncateStarterBench")
    logger.debug(s"Execute truncateOrder | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    preparedTruncate.executeUpdate() match {
      case 1 =>
        logger.debug(s"Success truncateOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        1
      case 0 =>
        logger.debug(s"Failed truncateOrder() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
  }

}
