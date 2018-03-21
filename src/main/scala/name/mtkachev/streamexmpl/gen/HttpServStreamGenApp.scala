package name.mtkachev.streamexmpl.gen

import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import name.mtkachev.streamexmpl.Transaction
import name.mtkachev.streamexmpl.TxJsonFormat._

import scala.io.StdIn

object HttpServStreamGenApp extends App with SprayJsonSupport {
  val conf = ConfigFactory.load("app")
  implicit val system = ActorSystem.create("http-serv", conf)
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val start = ByteString.empty
  val sep = ByteString("\n")
  val end = ByteString.empty

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
    EntityStreamingSupport.json().withFramingRenderer(Flow[ByteString].intersperse(start, sep, end))

  val random = new scala.util.Random()
  val txSrc: Source[Transaction, NotUsed] = Source
    .fromIterator(TransactionGen.stream(random, LocalDateTime.now()).iterator _)

  val route =
    path("pull") {
      get {
        complete(txSrc)
      }
    } ~
    path("push") {
      get {
/*
        Marshal(txSrc).to[RequestEntity]
        Multipart.FormData(
          Source.single(Multipart.FormData.BodyPart.fromPath(name, contentType, file, chunkSize))
        )
*/
        val txEnc: Source[ByteString, NotUsed] =
          txSrc.map {tx: Transaction => ByteString(txFormat.write(tx).prettyPrint)}
        val txEnt = HttpEntity.Chunked.fromData(MediaTypes.`application/json`, txEnc )
        val reqF = Http().singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = "http://localhost:8081",
            entity = txEnt
          )
        )
        complete(
          reqF.map { _ =>
            "done"
          }
        )
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}