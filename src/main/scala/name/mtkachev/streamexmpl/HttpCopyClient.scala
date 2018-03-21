package name.mtkachev.streamexmpl

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

object HttpCopyClient extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val httpSource = args(0)
  val outFile = args(1)

  def txStream2: Future[Source[ByteString, Any]] =
    Http().singleRequest(request = HttpRequest(uri = Uri(httpSource))).map { resp =>
      resp.entity.withoutSizeLimit.dataBytes.via(
        Framing.delimiter(ByteString("\n"),
          maximumFrameLength = 10000, allowTruncation = true)
      )
    }

  txStream2.map { txs =>
    txs.runWith(FileIO.toPath(new File(outFile).toPath))
  }
}