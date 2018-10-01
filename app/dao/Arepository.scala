package dao

import models.{AvailableProduct, Customer, Product}


trait AProductRepository {

  def getProductByProductId(id: String): Option[Product]

  def getProductByListProductID(productsID: List[String]): List[Option[Product]]

}

trait AProductOrderRepository {

  def insert(orderID: String, productID: String, quantity: Int): Int

  def delete(OrderID: String, productID: String, quantity: Int): Int

  def truncateProductOrder(): Int

  def getNumberOfproductOrders(): Int
}

trait AOrderRepository {

  def insert(orderID: String,customerID: String): Int

  def validationOrder(orderID: String): Int

  def truncateOrder(): Int

  def getNumberValidatedOrder(): Int

}

trait AAvailableProductRepository {

  def getProductsWithOffset(offset: Int, limit: Int): Option[List[AvailableProduct]]

}

trait ACustomerRepository {

  def getAllCustomers(): Option[List[Customer]]
}

trait AStockRepository {

  def updateAllStockQuantity(): Int

}

trait AStarterBenchRepository {

  def insertStarterBench(id: String, b: Boolean): Int

  def getStarterBench(): Boolean
  
  def truncateStarterBench(): Int

}

case class Repositories(
                         productRepository: AProductRepository,
                         productOrderRepository: AProductOrderRepository,
                         orderRepository: AOrderRepository,
                         availableProductRepository: AAvailableProductRepository,
                         customerRepository: ACustomerRepository,
                         stockRepository: AStockRepository,
                         benchRepository: AStarterBenchRepository
                       ) {

  def handleRequestError(exception: Exception) : Unit = ()

}
