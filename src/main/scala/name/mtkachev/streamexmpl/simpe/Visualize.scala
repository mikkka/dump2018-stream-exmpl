package name.mtkachev.streamexmpl.simpe

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Attributes}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global

object Visualize extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val graph =
    Source(0 to 4)
        .named("my source")
      .map { x => x + 1 }
        .named("map async + 1")
      .mapConcat { x => List.fill(x)(x) }
        .named("map async x x")
      .toMat(
        Sink.foreach{x: Int => println(x)}
          .named("my sink")
      )(Keep.right)

  val res = graph.run()

  res.onComplete(_ => system.terminate())
}
