package tf.bug.hexstudio.number

import cats.effect.IO
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import org.http4s.{EntityDecoder, Uri}
import org.http4s.client.Client

object HexxyApi {

  final case class Response(pattern: String, number: Double)

  object Response {
    given responseJsonCodec: JsonValueCodec[Response] = JsonCodecMaker.make
    given responseEntityDecoder: EntityDecoder[IO, Response] = tf.bug.hexstudio.jsonDecoder
  }

  final val numberApiBase = Uri.unsafeFromString("https://hexxy.media/api/v0/pattern/number/")

  def lookup(http: Client[IO], number: Double): IO[String] = {
    http.expect[Response](numberApiBase.addPath(number.toString))
      .map(_.pattern)
  }

}
