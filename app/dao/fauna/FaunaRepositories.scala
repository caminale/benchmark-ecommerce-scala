package dao.fauna

import dao.Repositories
import faunadb.FaunaClient

class FaunaRepositories  extends {

  val connection : FaunaClient = FaunaFactory.getConnection()

} with Repositories(new FaunaProductRepository(connection),
                    new FaunaProductOrderRepository(connection),
                    new FaunaOrderRepository(connection),
                    new FaunaAvailableProductRepository(connection),
                    new FaunaCustomerRepository(connection),
                    new FaunaStockRepository(connection),
                    new FaunaStarterBenchRepository(connection))
