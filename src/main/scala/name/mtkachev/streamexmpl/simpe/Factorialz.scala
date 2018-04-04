package name.mtkachev.streamexmpl.simpe

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Factorialz extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 50)

  val factorials = source
    .scan(BigInt(1))((acc, next) ⇒ acc * next)

  def lineSink(filename: String):
  Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s ⇒ ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  val res = factorials.map(_.toString).drop(1).runWith(lineSink("fctrlz.txt"))

  res.onComplete( _ => system.terminate())
}
