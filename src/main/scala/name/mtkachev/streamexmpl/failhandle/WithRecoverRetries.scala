package name.mtkachev.streamexmpl.failhandle

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global

object WithRecoverRetries extends App {
  val conf = ConfigFactory.load("app")
  implicit val system = ActorSystem("app", conf)
  implicit val mat = ActorMaterializer()

  val source = Source(0 to 10)
    .named("my source")
    .map { x => x + 1 }

  val failingSource = source
    .map {x => if (x % 3 == 0) throw new IllegalArgumentException(s"bad $x") else x }

  val graph = failingSource
      .named("map async + 1")
      .async("map_1_dispatcher", 32)
      .recoverWithRetries(1, {
        case _ => source
      })
      .mapConcat { x => List.fill(x)(x) }
      .named("map async x x")
      .async("map_x_dispatcher", 16)
      .toMat(
        Sink.foreach{x: Int => println(x)}
          .named("my sink")
      )(Keep.right)

  val res = graph.run()

  res.onComplete(_ => system.terminate())
}
