package dao.fauna

import dao.AStockRepository
import faunadb.FaunaClient

class FaunaStockRepository(connection : FaunaClient) extends AStockRepository{
  //TODO implement updateAllStockQuantity
  override def updateAllStockQuantity(): Int = ???
}
