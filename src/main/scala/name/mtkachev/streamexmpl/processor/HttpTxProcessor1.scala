package name.mtkachev.streamexmpl.processor

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, GraphDSL, Merge}
import akka.stream.{ActorMaterializer, FlowShape}
import com.typesafe.config.ConfigFactory
import name.mtkachev.streamexmpl.TxJsonFormat._
import name.mtkachev.streamexmpl._

import scala.concurrent.duration._
import scala.io.StdIn

/**
  * handle inifinite upload
  */
object HttpTxProcessor1 extends App  {
  val conf = ConfigFactory.load("app")
  implicit val system = ActorSystem("http-processor1", conf)
  implicit val materializer = ActorMaterializer()

  val logFile = Paths.get(args(0))
  val outputFile = Paths.get(args(1))

  implicit val executionContext = system.dispatcher
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  val logicFlow = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val logic =
      builder.add(FraudLogic(1.seconds, 1.seconds, FraudDetector.detect, 4))
    val monOut = builder.add(Merge[String](2))

    logic.mon1Out ~> Flow[Int].map{x => s"got $x tx"} ~> monOut
    logic.mon2Out ~> Flow[Map[Fraud, Int]].map{x => s"fraud gisto: $x "} ~> monOut

    monOut ~> Util.lineSink(logFile)

    FlowShape(logic.in, logic.out)
  }

  val route =
    path("process") {
      entity(asSourceOf[Transaction]) { txs =>
        val uploadRes = txs.via(logicFlow)
          .map(x => s"${x._1} : ${x._2}")
          .runWith(Util.lineSink(outputFile))

        complete {
          uploadRes.map(ioRes => s"write res: $ioRes")
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8081)

  println(s"Processor online at http://localhost:8081/\nPress RETURN to stop...")
  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
