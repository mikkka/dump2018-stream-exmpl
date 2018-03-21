package name.mtkachev.streamexmpl

import akka.NotUsed
import akka.stream.FanOutShape.{Init, Name}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, ZipWith}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class FraudLogicShape(_init: Init[Transaction] = Name("PriorityWorkerPool"))
    extends FanOutShape[Transaction](_init) {

  def this(name: String) =
    this(Name[Transaction](name))

  def this(in: Inlet[Transaction],
           out0: Outlet[Int],
           out1: Outlet[Map[Fraud, Int]],
           out2: Outlet[(Transaction, Fraud)]) =
    this(FanOutShape.Ports(in, out0 :: out1 :: out2 :: Nil))

  protected override def construct(
      init: Init[Transaction]): FanOutShape[Transaction] =
    new FraudLogicShape(init)

  val mon1Out = newOutlet[Int]("mon1Out")
  val mon2Out = newOutlet[Map[Fraud, Int]]("mon2Out")
  val out = newOutlet[(Transaction, Fraud)]("out")
}

object FraudLogic {
  val MAX_MON_COUNT = 10000

  // todo использовать mat value
  // todo buffer strategy
  def apply(mon1Dur: FiniteDuration,
            mon2Dur: FiniteDuration,
            fraudDetector: Transaction => Future[Fraud],
            parallelism: Int): Graph[FraudLogicShape, NotUsed] = {

    val mon1Flow = Flow[Transaction]
      .groupedWithin(MAX_MON_COUNT, mon1Dur)
      .map(_.size)

    val mon2Flow = Flow[(Transaction, Fraud)]
      .groupedWithin(MAX_MON_COUNT, mon1Dur)
      .map { xs =>
        xs.groupBy(_._2)
          .map(x => x._1 -> x._2.size)
      }

    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val inbound = builder.add(Broadcast[Transaction](3))
      val outbound = builder.add(Broadcast[(Transaction, Fraud)](2))

      val zip = builder.add(
        ZipWith[Transaction, Fraud, (Transaction, Fraud)](_ -> _)
      )
      val mon1 = builder.add(mon1Flow)
      val mon2 = builder.add(mon2Flow)

      val detect = Flow[Transaction]
        .mapAsync(parallelism)(fraudDetector)

      inbound ~> mon1
      inbound ~> zip.in0
      inbound ~> detect ~> zip.in1

      zip.out ~> outbound

      outbound.out(0) ~> mon2

      new FraudLogicShape(inbound.in, mon1.out, mon2.out, outbound.out(1))
    }
  }
}