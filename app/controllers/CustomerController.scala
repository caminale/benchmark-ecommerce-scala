
package controllers

import java.util.UUID.randomUUID

import actions._
import com.typesafe.config.ConfigFactory
import dao.ManagerRequest
import dao.fauna.PopulateDB
import javax.inject.Inject
import models.{AvailableProduct, Customer}
import org.slf4j.MDC
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CustomerController @Inject()(config: Configuration)(controllerComponents: ControllerComponents, fauna: PopulateDB)
                                  (implicit executionContext: ExecutionContext) extends AbstractController(controllerComponents) {

  val logger: Logger = Logger(this.getClass())
  val r = scala.util.Random

  val availableProductRepository = new dao.AvailableProductRepository
  val productOrderRepository     = new dao.ProductOrderRepository
  val benchRepository            = new dao.StarterBenchRepository
  val orderRepository            = new dao.OrderRepository
  val productRepository          = new dao.ProductRepository
  val customerRepository         = new dao.CustomerRepository
  val stockRepository            = new dao.StockRepository

  // Clean database
  def resetData(): Unit = {
    Await.result(stockRepository.updateAllStockQuantity() ,2 minutes)
    Await.result(orderRepository.truncateOrder(),1 minutes)
    Await.result(productOrderRepository.truncateProductOrder(), 1 minutes)
    Await.result(benchRepository.insertStarterBench("starter", false), 1 minutes)
  }

  def closeConnections(): Unit = {
    /*
         ManagerRequest.closeConnections()
    */
  }

  def associateOrderToCustomer(selectedCustomersID: List[String]): List[(String, String)] = {
    selectedCustomersID.map(randomUUID().toString -> _)
  }

  // Initials conditions
  def init(nbrCustomer: Int, nbrProducts: Int): (List[AvailableProduct], List[String]) = {
    MDC.put("method", "init")
    val typeDb: String = ConfigFactory.load().getString("api.typeDB")
    val nbrProductsToAdd: Int = ConfigFactory.load().getInt("api.nbrProductsToAdd")
    val stockQuantity: Int = ConfigFactory.load().getInt("api.stockQuantity")
    val nbThreads: Int = ConfigFactory.load().getInt("dbs.nbThreads")
    logger.info(s"******* INIT BENCHMARK WITH $nbrCustomer CUSTOMERS | $nbrProducts PRODUCTS | TYPE DATABASE " +
      s"$typeDb | NUMBER PRODUCTS TO ADD : $nbrProductsToAdd | STOCK QUANTITY : $stockQuantity | NBR THREADS : $nbThreads *******")


    val allCustomersID: List[String] = Await.result(customerRepository.getAllCustomers(), 1 minutes)
      .getOrElse(List.empty[Customer])
      .map(_.id)

    val selectedCustomersID: List[String] = r.shuffle(allCustomersID).take(nbrCustomer)

    val totalProductsInDB: Int = config.underlying.getInt("dbs.nbrTotalProductsInDB")
    //Start Offset to not have the same product in all injectors
    val startOffset: Int = r.nextInt(totalProductsInDB - nbrProducts)

    val availableProducts: List[AvailableProduct] = Await.result(availableProductRepository.getProductsWithOffset(startOffset, nbrProducts), 300 seconds)
      .getOrElse(List.empty[AvailableProduct])

    (availableProducts, selectedCustomersID)
  }

  def prepareActions(productsID: List[String], orderID: String, customerID: String): List[CustomerAction] = {
    // Prepare actions :  Primary Actions(GetProducts, Open), SecondaryActions(addProducts,getProducts, ValidateOrder)

    val primaryActions: List[CustomerAction] = List(/*GetProducts(productsID), */OpenOrder(orderID, customerID, prepareSecondaryActions(productsID, orderID, customerID)))
    primaryActions
  }

  def prepareSecondaryActions(productsID: List[String], orderID: String, customerID: String): List[CustomerAction] = {
    // Prepare Secondary actions
    val secondaryActions: List[CustomerAction] = List(AddProduct(orderID, productsID), GetProducts(productsID), AddProduct(orderID, productsID), GetProducts(productsID), ValidateOrder(orderID))
    secondaryActions
  }

  def runCustomerAction(customersActions: List[CustomerAction]): Future[List[Unit]] =  {
    //Launch all actions sequentially but async
    MDC.put("method", "runCustomer")
    logger.debug(s"Excecute runCustomerAction(${customersActions.length}")
    Future.sequence(customersActions.map(runAction))
  }

  private def runAction(action : CustomerAction) : Future[Unit] = action match {

    case OpenOrder(orderID, customerID, secondaryActions) =>
      Thread.sleep(r.nextInt(config.underlying.getInt("api.timeout")))
      logger.debug("******* OPEN ORDER *******")
      val res: Future[Unit] = orderRepository.insert(orderID, customerID).flatMap {
        case 1 =>
          logger.debug("******* CREATED ORDER *******")
          executeActionsSequentially(secondaryActions)
        case 0 =>
          logger.error("******* ORDER NOT CREATED *******")
          Future{Unit}
      }
      res

    case AddProduct(orderID, productsID) =>
      logger.debug("******* ADD PRODUCT *******" + productsID + orderID)
      Future.sequence(productsID.map(productID => productOrderRepository.insert(orderID, productID, 1))).map(_=> Unit)

    case ValidateOrder(orderID) =>
      logger.debug("******* ACTION ORDER VALIDATE*******")
      orderRepository.validationOrder(orderID).map {
        case 1 => logger.debug("******* UPDATE ORDER TO VALIDATED SUCCESSFUL *******")
        case 0 => logger.debug("******* UPDATE ORDER TO VALIDATED FAILED *******")
      }

    case GetProducts(productsID) =>
/*
      Thread.sleep(r.nextInt(config.underlying.getInt("api.timeout")))
*/
      logger.debug("******* GET PRODUCT *******")

      productRepository.getProductByListProductID(productsID).map(x => x.map {
        case Some(product) => {
          logger.debug("******* GET PRODUCT SUCCESSFUL ******* ")
          product
        }
        case None => logger.debug("******* GET NO PRODUCT ******* ")
      }).map(_ => Unit)
  }

  private def executeActionsSequentially(actions: List[CustomerAction]): Future[Unit] = {
    actions.foldLeft(Future[Unit]()){
      case (future, action) => future.flatMap( _ =>runAction(action))
    }
  }

  def runBenchmark(customersActions: List[CustomerAction]): List[Unit] = {

    MDC.put("method", "main")
    logger.info(s"Start benchmark")

    // START
    // Pass the list to runAction
    val processCustomerActions: Future[List[Unit]] = runCustomerAction(customersActions)
    Await.result(processCustomerActions, Duration.Inf)
  }

  def mainRoute()= Action { implicit request =>

    resetData()
    closeConnections()

    import java.util.UUID.randomUUID


    val nbrCustomers: Int = config.underlying.getInt("api.nbrCustomers")
    val nbrProducts: Int = config.underlying.getInt("api.nbrProducts")
    val nbrProductsToAdd: Int = config.underlying.getInt("api.nbrProductsToAdd")

    var (availableProducts, selectedCustomersID): (List[AvailableProduct], List[String]) = init(nbrCustomers, nbrProducts)

    // Create list of all cutomers actions
    logger.debug("le nbr de avProducts " + availableProducts.size)




    availableProducts = r.shuffle(availableProducts)
    val orderCustomerDict: List[(String, String)] = associateOrderToCustomer(selectedCustomersID)

    val customersActions: List[CustomerAction] = for {
      orderCustomer <- orderCustomerDict
      customerActions <- {
        var i = (nbrProducts - nbrProductsToAdd - 1)
        if(i <= 0) i = 1
        val randomProductsIndex = r.nextInt(i)
        val primaryActions = prepareActions(availableProducts.slice(randomProductsIndex, randomProductsIndex + nbrProductsToAdd).map(_.id), orderCustomer._1, orderCustomer._2)
        primaryActions
      }
    } yield customerActions

    benchRepository.insertStarterBench(randomUUID().toString, true)

    var isReady: Boolean = false

    while(!isReady) {
      isReady = Await.result(benchRepository.getStarterBench(), 10 second)
      Thread.sleep(1000)
      isReady
    }

    val startTime = System.currentTimeMillis()

    runBenchmark(customersActions)

    val totalTime: Int = (System.currentTimeMillis() - startTime).toInt
    logger.info(s"total time benchmark is : ${totalTime} ms")

    getResultsQueries()
    Await.result(benchRepository.truncateStarterBench(), 1 minutes)

    Ok
  }

  def cleanDatabase() = Action { implicit request =>
    resetData()
    closeConnections()
    Ok
  }

  def setUpFauna() = Action { implicit request =>
    fauna.main()
    Ok
  }

  def getResultsQueries(): Int = {
    Await.result(orderRepository.getNumberValidatedOrder(), 60 second)
    Await.result(productOrderRepository.getNumberOfproductOrders(), 60 second)
  }
}

