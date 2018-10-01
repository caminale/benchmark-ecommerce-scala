package dao.fauna

import com.typesafe.config.ConfigFactory
import faunadb.values.{Field, RefV}
import faunadb.{FaunaClient, query => q, values => v}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object FaunaFactory extends ContextExecution {



  val RefField = Field("ref").to[RefV]
  val TsField = Field("ts").to[Long]
  val ClassField = Field("class").to[RefV]
  val SecretField = Field("secret").to[String]

  // Page helpers
  case class Ev(ref: RefV, ts: Long, action: String)

  val EventField = Field.zip(
    Field("resource").to[RefV],
    Field("ts").to[Long],
    Field("action").to[String]
  ) map { case (r, ts, a) => Ev(r, ts, a) }

  val PageEvents = Field("data").collect(EventField)
  val PageRefs = Field("data").to[Seq[RefV]]

  def await[T](f: Future[T]) = Await.result(f, 5.second)

  def ready[T](f: Future[T]) = Await.ready(f, 5.second)

  def adminGetConnection(): FaunaClient = {
    FaunaClient(
      secret = ConfigFactory.load().getString("dbs.fauna.secretKey")
    )
  }
  def getConnection(): FaunaClient = {
    FaunaClient(
      secret = createKey()
    )
  }

  def closeConnection(client: FaunaClient) {
    client.close()
  }

  def createKey(nameDatabase: String = "octo"): String = {

    val adminClient = adminGetConnection()
    val keyResponse = adminClient.query(
      q.CreateKey(q.Obj("database" -> q.Database(nameDatabase), "role" -> "server"))
    )

    val key = await(keyResponse)
    val dbSecret = key(v.Field("secret").to[String]).get
    adminClient.close()

    dbSecret
  }

}