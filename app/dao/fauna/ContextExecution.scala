package dao.fauna

import scala.concurrent.ExecutionContext

trait ContextExecution {
  implicit val ec: ExecutionContext = new ExecutionContext {
    override def execute(runnable: Runnable): Unit = runnable.run()
    override def reportFailure(cause: Throwable): Unit = ExecutionContext.defaultReporter(cause)
  }
}
