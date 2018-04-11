package name.mtkachev.streamexmpl.failhandle

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object WithRestartFlow extends App {
  val conf = ConfigFactory.load("app")
  implicit val system = ActorSystem("app", conf)
  implicit val mat = ActorMaterializer()

  val source =
    Source(0 to 10)
      .named("my source")
      .map { x =>
        x + 1
      }

  val restartFlow = RestartFlow.onFailuresWithBackoff(
    minBackoff = 3.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2, // adds 20% "noise" to vary the intervals slightly
    maxRestarts = 3 // limits the amount of restarts to 20
  ) { () =>
    Flow[Int]
      .map { x =>
        x + 1
      }
      .map { x =>
          if (x % 5 == 0) throw new IllegalArgumentException(s"bad $x") else x
      }.named("map async + 1")
  }

  val graph = source
    .via(restartFlow)
    .async("map_1_dispatcher", 32)
    .recover {
      case _ => 88
    }
    .mapConcat { x => List.fill(x)(x) }
    .named("map async x x")
    .async("map_x_dispatcher", 16)
    .toMat(
      Sink.foreach { x: Int => println(x) }
        .named("my sink")
    )(Keep.right)

  val res = graph.run()

  res.onComplete(_ => system.terminate())
}
