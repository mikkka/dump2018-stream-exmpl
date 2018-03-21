package name.mtkachev.streamexmpl.processor

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape}
import akka.util.ByteString
import name.mtkachev.streamexmpl._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object FileTxProcessor extends App {
  val inputFile = Paths.get(args(0))
  val logFile1 = Paths.get(args(1))
  val logFile2 = Paths.get(args(2))
  val outputFile = Paths.get(args(3))

  implicit val system = ActorSystem("processor")
  implicit val mat = ActorMaterializer()

  val logicFlow = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val logic =
      builder.add(FraudLogic(1.seconds, 1.seconds, FraudDetector.detect, 4))

    logic.mon1Out ~> Flow[Int].map{x => s"got $x tx"} ~> Util.lineSink(logFile1)
    logic.mon2Out ~> Flow[Map[Fraud, Int]].map{x => s"fraud gisto: $x "} ~> Util.lineSink(logFile2)

    FlowShape(logic.in, logic.out)
  }

  val res = FileIO.fromPath(inputFile)
    .via(Framing.delimiter(ByteString(System.lineSeparator),
      maximumFrameLength = 4000, allowTruncation = true))
    .map(x => Transaction.fromString(x.utf8String))
    .mapConcat(_.toList)
    .via(logicFlow)
    .map(x => s"${x._1} : ${x._2}")
    .runWith(Util.lineSink(outputFile))

  res.onComplete(x => println("ends with " + x))
}