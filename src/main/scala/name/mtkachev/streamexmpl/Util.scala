package name.mtkachev.streamexmpl

import java.nio.file.Path

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink}
import akka.util.ByteString

import scala.concurrent.Future

object Util {
  def lineSink(file: Path): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s ⇒ ByteString(s + "\n"))
      .toMat(FileIO.toPath(file))(Keep.right)
}
