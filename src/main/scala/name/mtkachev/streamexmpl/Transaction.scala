package name.mtkachev.streamexmpl

import java.time.LocalDateTime
import java.util.Calendar

import name.mtkachev.streamexmpl.common.Amount

import scala.util.Try

object common {
  type Amount = BigDecimal

  def today = Calendar.getInstance.getTime
}

sealed trait TransactionType
case object Debit extends TransactionType
case object Credit extends TransactionType


object TransactionType {
  def fromString(s: String) = s match {
    case "+" => Some(Debit)
    case "-" => Some(Credit)
    case _ => None
  }

  def toString(tt: TransactionType) = tt match {
    case Debit => "+"
    case Credit => "-"
    case _ => None
  }
}

case class Transaction (
  id: String,
  accountNo: String,
  debitCredit: TransactionType,
  amount: Amount,
  date: LocalDateTime
)

object Transaction {
  def fromString(s: String): Option[Transaction] = {
    val split = s.split(';')
    if (split.length == 5) {
      for {
        tt <- TransactionType.fromString(split(2))
        amount <- Try(BigDecimal(split(3))).toOption
        date <- Try(LocalDateTime.parse(split(4))).toOption
      } yield Transaction(split(0), split(1), tt, amount, date)
    } else None
  }

  def toString(tx: Transaction) = s"${tx.id};${tx.accountNo};${TransactionType.toString(tx.debitCredit)};${tx.amount};${tx.date}"
}

sealed trait Fraud
case object Fraud0 extends Fraud {
  override def toString: String = "Fraud0"
}
case object Fraud30 extends Fraud{
  override def toString: String = "Fraud30"
}
case object Fraud70 extends Fraud{
  override def toString: String = "Fraud70"
}
case object Fraud100 extends Fraud{
  override def toString: String = "Fraud100"
}

