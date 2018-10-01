package dao.spanner

import java.util.{ArrayList, Collections}
import com.google.cloud.spanner._
import dao.AStarterBenchRepository
import org.slf4j.MDC
import play.api.Logger

class SpannerStarterBenchRepository(connection: DatabaseClient) extends AStarterBenchRepository {
  val logger: Logger = Logger(this.getClass)
  
  override def insertStarterBench(id: String, b: Boolean): Int = {
    MDC.put("method", "insertStarterBench")
    
    val mutation = Mutation.newInsertOrUpdateBuilder("bench")
            .set("id").to(id)
            .set("is_ready").to(b)
            .build
    
    
    try {
      logger.debug(s"Excecute insertStarterBench() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
      connection.write(Collections.singleton(mutation))
      logger.debug(s"Success insertStarterBench() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
      1
    } catch {
      case _: SpannerException =>
        logger.error(s"Failed insertStarterBench() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
        0
    }
    
  }
  
  override def getStarterBench(): Boolean = {
    MDC.put("method", "getStarterBench")
    logger.debug(s"Execute getStarterBench() | Connection :  ${connection} | ThreadID : ${Thread.currentThread().getName}")
    
    val getNumberOfValidatedOrder: Statement = Statement.of("SELECT is_ready FROM bench WHERE id = 'starter'")
    val rs: ResultSet = connection.singleUse().executeQuery(getNumberOfValidatedOrder)
    
    val isReady: Boolean = if (rs.next) {
      val res: Boolean = rs.getBoolean(0)
      res
    } else false
    
    isReady
  }
  
  override def truncateStarterBench(): Int = {
    MDC.put("method", "truncateStarterBench")
    
    val mutations = new ArrayList[Mutation]
    mutations.add(Mutation.delete("bench", KeySet.all))
    
    try {
      connection.write(mutations) match {
        case ts =>
          logger.debug(s"StarterBench deleted with success at ${ts.toDate}")
          1
      }
    } catch {
      case _: SpannerException =>
        logger.error("Deleted failed...")
        0
    }
  }
}
