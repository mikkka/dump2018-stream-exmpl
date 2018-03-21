package name.mtkachev.streamexmpl.gen

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import name.mtkachev.streamexmpl.TxJsonFormat._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

object HttpStreamGenApp extends App {
  val conf = ConfigFactory.load("app")
  implicit val system = ActorSystem("http-stream-gen", conf)
  implicit val mat = ActorMaterializer()

  val url = args(0)
  val lineCount = args(1).toInt

  val random = new scala.util.Random()

  val txSrc = Source
    .fromIterator(TransactionGen.stream(random, LocalDateTime.now()).iterator _)
    .take(lineCount)

  val bsSrc = txSrc.map(tx => tx.toJson.prettyPrint).map(ByteString(_))
  val req = HttpRequest(uri = Uri(url),
    entity = HttpEntity.Chunked.fromData(
      contentType = ContentTypes.`application/json`,
      bsSrc
    )
  )

  Http().singleRequest(req).onComplete {cmplt =>
    println("cmplt :" + cmplt)
    system.terminate()
  }
}
