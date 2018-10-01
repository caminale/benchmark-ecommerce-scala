package models

sealed trait Model

//Entities

case class Customer(id: String, name: String, email: String) extends Model
case class Manufacturer(id: String, name: String, location: String) extends Model




//Tables

case class Product(id: String, name: String, category: String, brand: String, description: String, uri: String, price: Double, visible: Boolean) extends Model
case class Order(id: String, customerID: String, orderedDate: String, state: String) extends Model
case class Stock(id: String, productID: String, stock: Int, location: String, lastDelivery: String, nextDelivery: String) extends Model



//Transition Tables

case class ProductOrder(id: String, productID: String, orderID: String, quantity: Int) extends Model
case class AvailableProduct(id: String, name: String, category: String, brand: String, description: String, uri: String, price: Double, stock: Int, location: String, nextDelivery: String) extends Model



