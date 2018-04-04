package name.mtkachev.streamexmpl

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Framing, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object DetectSimple extends App {
  val inputFile = Paths.get(args(0))
  val outputFile = Paths.get(args(3))

  implicit val system = ActorSystem("processor")
  implicit val mat = ActorMaterializer()

  val source: Source[Transaction, Future[IOResult]] =
    FileIO.fromPath(inputFile)
    .via(Framing.delimiter(ByteString(System.lineSeparator),
      maximumFrameLength = 4000, allowTruncation = true))
    .map(x => Transaction.fromString(x.utf8String))
    .mapConcat(_.toList)

  val sink: Sink[String, Future[IOResult]] =
    Util.lineSink(outputFile)

  val matValue = source
    .map(tx => tx -> FraudDetector.detectSync(tx))
    .map(x => s"${x._1} : ${x._2}")
    .runWith(sink)

  matValue.onComplete(_ => system.terminate())
}
