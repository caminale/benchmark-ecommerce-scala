package dao

import models.{AvailableProduct, Customer, Product}

import scala.concurrent.Future

class ProductRepository {

  def getProductByProductId(id: String): Future[Option[Product]] =
    ManagerRequest.executeRequest(_.productRepository.getProductByProductId(id), "getProductByProductId")

  def getProductByListProductID(productsID: List[String]): Future[List[Option[Product]]] =
    ManagerRequest.executeRequest(_.productRepository.getProductByListProductID(productsID), "getProductByListProductID")

}

class ProductOrderRepository {

  def insert(orderID: String, productID: String, quantity: Int): Future[Int] =
    ManagerRequest.executeRequest(_.productOrderRepository.insert(orderID, productID, quantity), "insertProductOrder")

  def delete(orderID: String, productID: String, quantity: Int): Future[Int] =
    ManagerRequest.executeRequest(_.productOrderRepository.delete(orderID, productID, quantity), "deleteProductOrder")

  def truncateProductOrder(): Future[Int] =
    ManagerRequest.executeRequest(_.productOrderRepository.truncateProductOrder(), "truncateProductOrder")

  def getNumberOfproductOrders(): Future[Int] =
    ManagerRequest.executeRequest(_.productOrderRepository.getNumberOfproductOrders(), "getNumberOfproductOrders")

}

class OrderRepository {

  def insert(orderID: String,customerID: String): Future[Int] =
    ManagerRequest.executeRequest(_.orderRepository.insert(orderID, customerID), "insertOrder")

  def validationOrder(orderID: String): Future[Int] =
    ManagerRequest.executeRequest(_.orderRepository.validationOrder(orderID), "validationOrder")

  def truncateOrder(): Future[Int] =
    ManagerRequest.executeRequest(_.orderRepository.truncateOrder(), "truncateOrder")

  def getNumberValidatedOrder(): Future[Int] =
    ManagerRequest.executeRequest(_.orderRepository.getNumberValidatedOrder(), "getNumberValidatedOrder")

}

class AvailableProductRepository {

  def getProductsWithOffset(offset: Int, limit: Int): Future[Option[List[AvailableProduct]]] =
    ManagerRequest.executeRequest(_.availableProductRepository.getProductsWithOffset(offset, limit),"getProductsWithOffset")
}
class CustomerRepository {

  def getAllCustomers(): Future[Option[List[Customer]]] =
    ManagerRequest.executeRequest(_.customerRepository.getAllCustomers(), "getAllCustomers")

}
class StockRepository {

  def updateAllStockQuantity(): Future[Int] =
    ManagerRequest.executeRequest(_.stockRepository.updateAllStockQuantity(), "updateAllStockQuantity")

}

class StarterBenchRepository {

  def getStarterBench(): Future[Boolean] =
    ManagerRequest.executeRequest(_.benchRepository.getStarterBench(), "getStarterBench")

  def insertStarterBench(id: String, b: Boolean): Future[Int] =
    ManagerRequest.executeRequest(_.benchRepository.insertStarterBench(id, b), "insertStarterBench")
  
  def truncateStarterBench(): Future[Int] =
    ManagerRequest.executeRequest(_.benchRepository.truncateStarterBench(), "truncateStarterBench")

}



