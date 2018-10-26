package dao


import java.util.concurrent.{Executors, TimeUnit}

import com.google.cloud.spanner.v1.SpannerClient
import com.google.spanner.v1.SpannerGrpc.SpannerStub
import com.typesafe.config.ConfigFactory
import dao.cockroach.CockroachRepositories
import dao.fauna.FaunaRepositories
import dao.spanner.SpannerRepositories
import dao.postgres.PostgresRepositories

import org.slf4j.MDC
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future, Promise}
object ManagerRequest {

  // TODO convertir currentTimeMillis -> currentTimeNanos
  // TODO ajouter une chaine pour identifier la fonction à timer
  // TODO ajouter les logs concernant les metrics

  val logger:Logger = Logger(this.getClass)

  lazy val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(ConfigFactory.load().getInt("dbs.nbThreads")))

  val database: Database = databaseMatching()

  def getDatabaseRepositories(): Repositories = database match {
    case Cockroach() => new CockroachRepositories()
    case Fauna()     => new FaunaRepositories()
    case Spanner()   => new SpannerRepositories()
    case Postgres()   => new PostgresRepositories()
  }

  val local : ThreadLocal[Repositories] = ThreadLocal.withInitial[Repositories](() => getDatabaseRepositories())

  def resetThreadLocalRepositories(): Unit = {
    local.set(getDatabaseRepositories())
  }

  def executeRequest[T](f: Repositories => T, nameRequest: String): Future[T] = {
    MDC.put("method", "executeRequest")

    val promise = Promise[T]()

    ec.execute(() => {

      val startTime = System.currentTimeMillis()
      try {

        val res = f(local.get())
        val totalTime: Int = (System.currentTimeMillis() - startTime).toInt
        logger.info(s"Request type : $nameRequest | took : $totalTime")
        promise.success(res)

      } catch {

        case e : Exception =>
          local.get().handleRequestError(e)
          val res = f(local.get())
          val totalTime: Int = (System.currentTimeMillis() - startTime).toInt
          logger.info(s"Request type : $nameRequest | took : $totalTime")
          promise.success(res)

      }
    })
    promise.future
  }

  def databaseMatching(): Database = {
    val typeDB: String = ConfigFactory.load().getString("api.typeDB")
    typeDB match {
      case "cockroach" => Cockroach()
      case "fauna" => Fauna()
      case "spanner" => Spanner()
      case "postgres" => Postgres()
    }
  }

  def closeConnections(): Unit = {
    val typeDB: String = ConfigFactory.load().getString("api.typeDB")
    typeDB match {

      case "cockroach" =>
        for(_ <- 1 to ConfigFactory.load().getInt("dbs.nbThreads")) {
          ec.execute(() => {
            logger.info(Thread.currentThread().getId + Thread.currentThread().getName + " close connection : " + local.get())
            local.remove()
          })
        }

      case "fauna" => ???

      case "spanner" =>
        val instanceID = ConfigFactory.load().getString("dbs.spanner.instanceID")
        val databaseID = ConfigFactory.load().getString("dbs.spanner.databaseID")
        val projectID  = ConfigFactory.load().getString("dbs.spanner.projectID")

/*        val spannerClient = SpannerClient.create
        var cpt = 0

        import scala.collection.JavaConversions._
  
        for(element <- spannerClient.listSessions(s"projects/$projectID/instances/$instanceID/databases/$databaseID").iterateAll()) {
          //logger.info("DELETE SESSION NAME : "+element.getName)
          cpt += 1
  
          spannerClient.deleteSession(element.getName)
        }

        logger.info("NUMBER DELETED SESSION: " + cpt)
        */
/*
        spannerClient.createSession(s"projects/$projectID/instances/$instanceID/databases/$databaseID")
*/
/*
        while(! spannerClient.awaitTermination(5,TimeUnit.SECONDS)) Thread.sleep(100)
*/
    }
  }
}

