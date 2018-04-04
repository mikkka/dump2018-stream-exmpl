package name.mtkachev.streamexmpl.simpe

import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.concurrent.{Future, Promise}

class FirstValue[A] extends GraphStageWithMaterializedValue[FlowShape[A, A], Future[A]] {
  val in = Inlet[A]("FirstValue.in")
  val out = Outlet[A]("FirstValue.out")
  val shape = FlowShape.of(in, out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[A]) = {
    val promise = Promise[A]()
    val logic = new GraphStageLogic(shape) {
      var isFirst = true

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val elem = grab(in)
          if (isFirst) {
            isFirst = false
            promise.success(elem)
          }
          push(out, elem)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })
    }

    (logic, promise.future)
  }
}