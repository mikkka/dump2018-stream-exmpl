package name.mtkachev.streamexmpl

import java.time.LocalDateTime

import spray.json._

object TxJsonFormat
  extends akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
    with spray.json.DefaultJsonProtocol {

  implicit val txTypeFormat = new JsonFormat[TransactionType] {
    override def write(obj: TransactionType): JsValue = obj match {
      case Debit => JsString("d")
      case Credit => JsString("c")
    }

    override def read(json: JsValue): TransactionType = json match {
      case JsString("d") => Debit
      case JsString("c") => Credit
      case _ => deserializationError("not valid debit/credit")
    }
  }

  implicit val lDateTimeFormat = new JsonFormat[LocalDateTime] {
    override def write(obj: LocalDateTime): JsValue =
      JsString(obj.toString)

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(str) => LocalDateTime.parse(str)
      case _ => deserializationError("not valid debit/credit")
    }
  }

  implicit val txFormat = jsonFormat5(Transaction.apply)
}
