package io.findify.metrics.exporter.pulsar.pulsar

import cats.effect.IO
import org.apache.pulsar.client.api.{ MessageId, Producer, PulsarClient, Schema }

import java.util.concurrent.TimeUnit
case class RawMessage(key: Option[String], data: Array[Byte])

object PulsarOutput {

  def apply(pulsarClient: PulsarClient, namespace: String, topic: String): Iterator[RawMessage] = {
    val reader = pulsarClient
      .newReader()
      .readerName(
        s"compaction-tracer-reader-${System.currentTimeMillis()}"
      )
      .topic(s"${namespace}${topic}")
      .startMessageId(MessageId.earliest)
      .readCompacted(true)
      .receiverQueueSize(1000)
      .create()

    new Iterator[RawMessage] {
      override def hasNext: Boolean = reader.hasMessageAvailable

      override def next(): RawMessage = {
        val m = Option(reader.readNext(20, TimeUnit.SECONDS))
        RawMessage(m.map(_.getKey), m.map(_.getData).fold(Array.empty[Byte])(identity))
      }
    }
  }
}

object PulsarInput {
  def apply(pulsarClient: PulsarClient, namespace: String, topic: String) =
    pulsarClient
      .newProducer(Schema.BYTES)
      .topic(s"${namespace}${topic}")
      .producerName(s"compaction-tracer-producer-${System.currentTimeMillis()}")
      .enableBatching(false)
      .create()

  def doEmit(pulsarClient: Producer[Array[Byte]], key: String, payload: Option[Array[Byte]]): IO[MessageId] =
    for {
      _ <- IO(println(s"Emitting: ${key}"))
      m <- IO(
        payload
          .foldLeft(
            pulsarClient
              .newMessage()
              .key(key)
          ) { (producer, data) =>
            producer.value(data)
          }
          // Use the synchronous send method here, which does not enqueue messages, but rather blocks until
          // the write is confirmed by the broker, returning MessageId
          .send()
      )
      _ <- IO(println(s"Emitted: ${key}"))
    } yield (m)

}
