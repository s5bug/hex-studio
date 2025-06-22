package tf.bug

import cats.Applicative
import cats.effect.Concurrent
import com.github.plokhotnyuk.jsoniter_scala.core.*
import org.http4s.{Charset, EntityDecoder, EntityEncoder, MediaType}
import org.http4s.headers.`Content-Type`

package object hexstudio {

  given jsonEncoder[F[_], A](using jsonCodec: JsonValueCodec[A]): EntityEncoder[F, A] =
    EntityEncoder
      .byteArrayEncoder[F]
      .contramap(writeToArray(_: A))
      .withContentType(`Content-Type`(MediaType.application.json, Some(Charset.`UTF-8`)))

  given jsonDecoder[F[_], A](using jsonCodec: JsonValueCodec[A], concurrent: Concurrent[F]): EntityDecoder[F, A] =
    EntityDecoder
      .byteArrayDecoder[F]
      .map(readFromArray[A](_))

}
