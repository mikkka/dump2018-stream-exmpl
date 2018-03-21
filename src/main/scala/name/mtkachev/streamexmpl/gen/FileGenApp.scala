package name.mtkachev.streamexmpl.gen

import java.nio.file.Paths
import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import name.mtkachev.streamexmpl.{Transaction, Util}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object FileGenApp extends App {
  import akka.stream.scaladsl._

  implicit val system = ActorSystem("processor")
  implicit val mat = ActorMaterializer()

  val file = Paths.get(args(0))
  val lineCount = args(1).toInt

  println(file)

  val random = new scala.util.Random()

  val res = Source
    .fromIterator(TransactionGen.stream(random, LocalDateTime.now()).iterator _)
    .map(tx => Transaction.toString(tx))
    .take(lineCount)
    .runWith(Util.lineSink(file))

  res.onComplete {
    case Success(s) =>
      println("success :" + s)
      system.terminate()
    case Failure(s) =>
      println("failure :" + s)
      system.terminate()
  }
}
