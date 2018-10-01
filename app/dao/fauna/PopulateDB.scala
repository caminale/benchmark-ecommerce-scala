package dao.fauna

import java.sql.Timestamp
import java.util.UUID.randomUUID

import dao.fauna.FaunaFactory._
import faunadb.values.Value
import faunadb.{FaunaClient, query => q, values => v}
import javax.inject.Inject
import models.{AvailableProduct, Customer, Product, Stock}
import play.api.libs.json._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class PopulateDB @Inject()(env: play.api.Environment) {

  val productsPath: String = "./populateDB/products-short-cd.json"

  implicit val customerCodec = v.Codec.caseClass[Customer]
  implicit val productCodec = v.Codec.caseClass[Product]
  implicit val stockCodec = v.Codec.caseClass[Stock]
  implicit val availableProductCodec = v.Codec.caseClass[AvailableProduct]

  def createDatabase(nameDatabase: String): Unit = {

    val adminClient = adminGetConnection()

    val createDBResponse = adminClient.query(
      q.If(
        q.Exists(q.Database(nameDatabase)),
        Value("Database already exist"),
        q.CreateDatabase(q.Obj("name" -> nameDatabase))
      )
    )

    Try(await(createDBResponse)) match {
      case Success(e: v.StringV) =>
        println(s"warning:  ${e.value}")
      case Failure(f) =>
        println(s"something strange happened: ${f.getMessage}")
      case Success(_: v.Value) =>
        println(s"******* DATABASE $nameDatabase SUCCESSFULLY CREATED")

    }
    adminClient.close()
  }

  def createKey(nameDatabase: String): String = {

    val adminClient = adminGetConnection()
    val keyResponse = adminClient.query(
      q.CreateKey(q.Obj("database" -> q.Database(nameDatabase), "role" -> "server"))
    )

    val key = await(keyResponse)
    val dbSecret = key(v.Field("secret").to[String]).get
    adminClient.close()

    dbSecret
  }

  def createClass(serverClient: FaunaClient, namesClasses: List[String]): Unit = {

    val createClassesResponse: Value = await(serverClient.query(
      q.Map(namesClasses,
        q.Lambda { name => {
          q.If(q.Exists(q.Class(name)),
            Value(""),
            q.CreateClass(q.Obj("name" -> name)

            )
          )
        }
        }
      )
    ))

    Try(createClassesResponse) match {
      case Failure(f) =>
        println(s"something strange happened: ${f.getMessage}")
      case Success(e: v.ArrayV) =>
        println(s"******* CLASSES ${namesClasses.toString} SUCCESSFULLY CREATED *******\n or already exists if you know where you are :) ")
    }
  }

  def importProducts(pathToData: String): List[Product] = {
    // TODO generalized for each product file
    val json: Option[JsValue] = env.resourceAsStream(productsPath).map(Json.parse(_))
    val finalJSON = json match {
      case Some(p) => (p \ "products")
    }

    @scala.annotation.tailrec
    def getProductList(jsonProduct: JsLookupResult, nb2Import: Int, i: Int = 0, list: List[Product] = Nil): List[Product] = {
      if (list.length < nb2Import) {
        val id = java.util.UUID.randomUUID().toString
        val name = (jsonProduct(i) \ "title").as[JsString].toString()
        val category = (jsonProduct(i) \ "categories").as[JsString].toString()
        val brand = (jsonProduct(i) \ "brand").as[JsString].toString()
        val description = (jsonProduct(i) \ "description").as[JsString].toString()
        var finalDescription = ""
        if (description.length >= 1000) {
          finalDescription = description.substring(0, 1000)
        } else {
          finalDescription = description
        }
        val uri = (jsonProduct(i) \ "imUrl").as[JsString].toString()
        val price = (jsonProduct(i) \ "price").as[JsNumber].as[Double]
        val visible = true
        val product = Product(id, name, category, brand, finalDescription, uri, price, visible)
        getProductList(jsonProduct, nb2Import, i + 1, product :: list)
      }
      else {
        list
      }
    }

    val productList = getProductList(finalJSON, 10)
    productList

  }

  def createIndicesToGetStockByProductID(client: FaunaClient): Unit = {

    val res = await(client.query(
      q.If(
        q.Exists(q.Index("stock_by_product_id")),
        Value("Index already exist"),
        q.CreateIndex(
          q.Obj(
            "name" -> "stock_by_product_id",
            "source" -> q.Class("stock"),
            "unique" -> true,
            "terms" -> q.Arr(q.Obj("field" -> q.Arr("data", "productID")))
          )
        )
      )
    )
    )
    Try(res) match {
      case Success(_: v.Value) =>
        println(s"******* INDEX stock_by_product_id SUCCESSFULLY INSERTED *******")
      case Success(e: v.StringV) =>
        println(s"warning:: ${e.value} (StockByProductID)")
      case Failure(f) =>
        println(s"something strange happened: ${f.getMessage}")
    }
  }

  def createIndices(client: FaunaClient, indexesByIdToBuild: Map[String, String], indexesFilterByIdToBuild: Map[String, String]): Unit = {
    /*
     * Create two indexes here. The first index is to query customers when you know specific id's.
     * The second is used to query customers by range. Examples of each type of query are presented
     * below.
     */

    var statementIndex: Iterable[Value] = null
    Try {
      statementIndex = indexesByIdToBuild.map {
        case (table, index) =>
          await(client.query(
            q.If(
              q.Exists(q.Index(index)),
              Value("Index already exist"),
              q.CreateIndex(
                q.Obj(
                  "name" -> index,
                  "source" -> q.Class(table),
                  "unique" -> true,
                  "terms" -> q.Arr(q.Obj("field" -> q.Arr("data", "id")))
                )
              )
            )
          )
          )
      }
      statementIndex = indexesFilterByIdToBuild.map {
        case (table, index) =>
          await(client.query(
            q.If(
              q.Exists(q.Index(index)),
              Value("Index already exist"),
              q.CreateIndex(
                q.Obj(
                  "name" -> index,
                  "source" -> q.Class(table),
                  "unique" -> true,
                  "values" -> q.Arr(
                    q.Obj("field" -> q.Arr("data", "id")),
                    q.Obj("field" -> q.Arr("ref")))
                )
              )
            )
          ))
      }
      statementIndex
    } match {
      case Failure(f) =>
        println(s"******* FAILURE INSERT INDEX *******\n error is: $f")
      case Success(e) =>
        println(s"******* INDEXES SUCCESSFULLY INSERTED  ******* \n or already exists if you know where you are")
    }
  }

  def createProducts(listProduct: List[Product], client: FaunaClient): Unit = {
    val result = Try {
      val stmt = client.query(
        q.Map(listProduct,
          q.Lambda { product =>
            q.Create(
              q.Class("product"),
              q.Obj("data" -> product)
            )
          }
        )
      )
      await(stmt)
    } match {
      case Success(_) =>
        println(s"******* PRODUCTS SUCCESSFULLY INSERTED *******")
      case Failure(e) =>
        println(s"******* FAILURE INSERT PRODUCT\n error: $e *******")
    }
  }

  def createStock(listStock: List[Stock], client: FaunaClient): Unit = {

    val result = Try {
      val stmt = client.query(
        q.Map(listStock,
          q.Lambda { stock =>
            q.Create(
              q.Class("stock"),
              q.Obj("data" -> stock)
            )
          }
        )
      )
      await(stmt)
    }

    result match {
      case Success(_) =>
        println(s"******* STOCK SUCCESSFULLY INSERTED   *******")
      case Failure(e) =>
        println(s"******* FAILURE INSERT STOCK\n error: $e *******")
    }
  }

  def createCustomers(client: FaunaClient, numCustomers: Int): Seq[v.RefV] = {
    /*
     * Create 'numCustomers' customer records with ids from 1 to 'numCustomers'
     */
    var custRefs: Seq[v.RefV] = null

    val jsonNames: Option[JsValue] = env.resourceAsStream("./data/names.json") map (Json.parse(_))

    val names: JsValue = jsonNames match {
      case Some(p_names: JsValue) => {
        p_names
      }
    }
    val namesList = names.as[List[String]].take(numCustomers)

    val customers: List[Customer] = namesList.map {
      name =>
        Customer(randomUUID().toString, name, name + "@octo.com")
    }


    val result = Try {
      val stmt = client.query(
        q.Map(customers,
          q.Lambda { customer =>
            q.Create(
              q.Class("customer"),
              q.Obj("data" -> customer)
            )
          }
        )
      )
      Await.result(stmt, Duration.Inf)
    }
    result match {
      case Success(s) => {
        println(s"$numCustomers customers successfully inserted")
        custRefs = s.collect(v.Field("ref").to[v.RefV]).get
      }
    }
    return custRefs
  }

  def populateDB(): Unit = {

    /** **** CREATE DB IF NOT EXISTS *****/

    val DB_NAME = "octo"
    createDatabase(DB_NAME)


    /** **** CREATE SERVER KEY & CONNECTION *****/

    // Create key to create and get client connection
    val dbSecretKey: String = createKey(DB_NAME)
    val client = getConnection()


    /** **** CREATE TABLES IF NOT EXISTS *****/

    val allClassToBuild: List[String] = List("customer", "product", "stock", "orders", "product_orders")
    createClass(client, allClassToBuild)


    /** **** CREATE INDEXES IF NOT EXISTS *****/

    val allIndexFilterToBuild: Map[String, String] = Map("customer" -> "customer_id_filter", "product" -> "product_id_filter", "stock" -> "stock_id_filter", "orders" -> "orders_id_filter", "product_orders" -> "product_orders_id_filter")
    val allIndexByIdToBuild: Map[String, String] = Map("customer" -> "customer_by_id", "product" -> "product_by_id", "stock" -> "stock_by_id", "orders" -> "orders_by_id", "product_orders" -> "product_orders_by_id")
    createIndices(client, allIndexByIdToBuild, allIndexFilterToBuild)

    createIndicesToGetStockByProductID(client)


    /** **** CREATE CUSTOMERS *****/

    val customerNumber = 5
    createCustomers(client, customerNumber)


    /** **** CREATE PRODUCTS & ASSOCIATED STOCKS *****/

    val cities: List[String] = List("France", "Italie", "Belgique", "Allemagne", "Chine")
    val dayInMs: Long = 86400000

    val products: List[Product] = importProducts(productsPath)

    val stocks: List[Stock] = products.map(product => Stock(java.util.UUID.randomUUID().toString,
      product.id, 100, cities(scala.util.Random.nextInt(cities.length)),
      new Timestamp(System.currentTimeMillis()).toString,
      new Timestamp(System.currentTimeMillis() + 30 * dayInMs).toString))

    createStock(stocks, client)
    createProducts(products, client)


    client.close
  }


  def main(): Unit = {

    populateDB()

  }
}
