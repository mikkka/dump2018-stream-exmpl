package name.mtkachev.streamexmpl

import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, Future}

object FraudDetector {
  implicit val ex: ExecutionContext = ExecutionContext.
    fromExecutor(Executors.newFixedThreadPool(4))

  def detect(tx: Transaction): Future[Fraud] = Future {
    (tx.accountNo.sum + tx.id.sum) % 4 match {
      case 0 => Fraud0
      case 1 => Fraud30
      case 2 => Fraud70
      case 3 => Fraud100
    }
  }

  def slowDetect(tx: Transaction): Future[Fraud] = Future {
    Thread.sleep(new (scala.util.Random).nextInt(100))

    (tx.accountNo.sum + tx.id.sum) % 4 match {
      case 0 => Fraud0
      case 1 => Fraud30
      case 2 => Fraud70
      case 3 => Fraud100
    }
  }

  def detectSync(tx: Transaction): Fraud =
    (tx.accountNo.sum + tx.id.sum) % 4 match {
      case 0 => Fraud0
      case 1 => Fraud30
      case 2 => Fraud70
      case 3 => Fraud100
    }
}
