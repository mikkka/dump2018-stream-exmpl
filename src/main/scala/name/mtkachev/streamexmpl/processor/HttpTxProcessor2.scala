package name.mtkachev.streamexmpl.processor

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Framing, GraphDSL, Merge, Source}
import akka.stream.{ActorMaterializer, FlowShape}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import name.mtkachev.streamexmpl.TxJsonFormat._
import name.mtkachev.streamexmpl._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

/**
  * pull source from httpSource with infinite client response
  */
object HttpTxProcessor2 extends App {
  val conf = ConfigFactory.load("app")
  implicit val system = ActorSystem("http-processor2", conf)
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  val httpSource = args(0)
  val outputFile = Paths.get(args(1))

  def logicFlow = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val logic =
      builder.add(FraudLogic(1.seconds, 1.seconds, FraudDetector.slowDetect, 4))

    val monOut = builder.add(Merge[String](2))

    logic.out ~>
      Flow[(Transaction, Fraud)]
        .map(x => s"${x._1} : ${x._2}") ~>
      Util.lineSink(outputFile)

    logic.mon1Out ~>
      Flow[Int].map { x =>
        s"got $x tx"
      } ~> monOut
    logic.mon2Out ~>
      Flow[Map[Fraud, Int]].map { x =>
        s"fraud gisto: $x "
      } ~> monOut

    FlowShape(logic.in, monOut.out)
  }

  def txStream: Future[Source[Transaction, Any]] =
    Http()
      .singleRequest(request = HttpRequest(uri = Uri(httpSource)))
      .map { resp =>
        resp.entity.withoutSizeLimit.dataBytes
          .via(
            Framing.delimiter(ByteString("\n"),
                              maximumFrameLength = 10000,
                              allowTruncation = true)
          )
          .mapAsync(4)(str => Unmarshal(str).to[Transaction])
      }

  val route =
    path("process") {
      onSuccess(txStream) { txs =>
        complete {
          HttpResponse(
            entity = HttpEntity.Chunked(
              ContentTypes.`text/plain(UTF-8)`,
              txs.via(logicFlow)
                .map(x ⇒ ByteString(x.toString + "\n---\n", "UTF8"))
            )
          )
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8081)

  println(
    s"Processor online at http://localhost:8081/\nPress RETURN to stop...")
  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
