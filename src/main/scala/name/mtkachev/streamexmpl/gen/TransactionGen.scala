package name.mtkachev.streamexmpl.gen

import Stream._
import java.time.LocalDateTime

import name.mtkachev.streamexmpl.{Credit, Debit, Transaction, TransactionType}
import name.mtkachev.streamexmpl.common.Amount

import scala.util.Random

object TransactionGen {
  private def id(rand: Random): String = new String(rand.alphanumeric.take(12).toArray)
  private def accountNo(rand: Random): String = "acc-" + new String(rand.alphanumeric.take(4).toArray)
  private def debitCredit(rand: Random): TransactionType = if (rand.nextBoolean()) Debit else Credit
  private def amount(rand: Random): Amount = rand.nextInt(100000) / 100.0
  private def date(rand: Random, base: LocalDateTime): LocalDateTime = base.plusNanos(rand.nextInt(2000000000))

  def next(r: Random, base: LocalDateTime) = Transaction(
    id(r),
    accountNo(r),
    debitCredit(r),
    amount(r),
    date(r, base)
  )

  def stream(r: Random, base: LocalDateTime): Stream[Transaction] = {
    val t = next(r, base)
    t #:: stream(r, t.date)
  }
}
