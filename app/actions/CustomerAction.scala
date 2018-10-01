package actions

sealed trait CustomerAction
case class GetProducts(productsID: List[String]) extends CustomerAction
case class CreateOrder(orderID: String, customerID: String) extends CustomerAction
case class AddProduct(orderID: String, productID: List[String]) extends CustomerAction
case class ValidateOrder(orderID: String) extends CustomerAction
case class OpenOrder(orderID: String, customerID: String, secondaryActions: List[CustomerAction]) extends CustomerAction

