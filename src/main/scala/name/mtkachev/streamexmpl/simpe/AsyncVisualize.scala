package name.mtkachev.streamexmpl.simpe

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Attributes}
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global

object AsyncVisualize extends App {
  val conf = ConfigFactory.load("app")
  implicit val system = ActorSystem("app", conf)
  implicit val mat = ActorMaterializer()

  val graph =
    Source(0 to 4)
      .named("my source")
      .map { x => x + 1 }
        .named("map async + 1")
        .async("map_1_dispatcher", 32)
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
