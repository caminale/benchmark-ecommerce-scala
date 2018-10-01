package dao.fauna

import dao.AStarterBenchRepository
import faunadb.FaunaClient

class FaunaStarterBenchRepository (connection: FaunaClient) extends AStarterBenchRepository {
  override def getStarterBench(): Boolean = ???

  override def insertStarterBench(id: String, b: Boolean): Int = ???
  
  override def truncateStarterBench(): Int = ???
}
