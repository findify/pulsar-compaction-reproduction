package io.findify.metrics.exporter.pulsar

import akka.actor.ActorSystem
import cats.effect.{ ExitCode, IO, IOApp }
import com.evolutiongaming.HttpServer
import com.evolutiongaming.metrics.{ MetricCollectors, MetricResources, Report }
import io.findify.metrics.exporter.pulsar.pulsar.{ PulsarInput, PulsarOutput }
import io.prometheus.client.{ Counter, Gauge }
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.shade.org.apache.commons.lang.SerializationUtils

import cats.syntax.flatMap._

import scala.concurrent.duration.DurationInt
object PulsarCompaction extends IOApp {

  implicit def toByteArray(value: Any): Array[Byte] = {
    val valueConverted: Array[Byte] = SerializationUtils.serialize(value.isInstanceOf[Serializable])
    valueConverted
  }

  val uniqueEvent = "unique"
  override def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(cs: MetricCollectors) <- IO(new MetricCollectors())
      implicit0(as: ActorSystem)      <- IO(ActorSystem("pc-metrics"))
      metricsRoute                    <- IO(new MetricResources(Report(cs)).route)
      _                               <- IO(new HttpServer(Seq(metricsRoute), "0.0.0.0", 8080))
      url                             <- IO(System.getenv("PULSAR_URL"))
      namespace                       <- IO(System.getenv("NAMESPACE"))
      topic                           <- IO(System.getenv("TOPIC"))
      roundBreak                      <- IO(System.getenv("ROUNDS_BREAK").toInt)
      _                               <- program(url, namespace, topic, cs, roundBreak)
    } yield ExitCode.Success

  def program(url: String, namespace: String, topic: String, cs: MetricCollectors, roundBreak: Int) =
    for {
      pulsarClient0 <- IO(PulsarClient.builder().serviceUrl(url).build())
      producer      <- IO(PulsarInput(pulsarClient0, namespace, topic))
      _             <- PulsarInput.doEmit(producer, uniqueEvent, Some(toByteArray("Unique Product")))
      _             <- IO(producer.close())
      _             <- IO(pulsarClient0.close())
      createDelete <- IO(
        cs.registerCounter(
          _.name("findify_pulsar_compaction_tc_ops")
            .labelNames("ops")
            .help("Compaction issue tracer delete/create count")
        )
      )
      compactedTopicSize <- IO(
        cs.registerGauge(
          _.name("findify_pulsar_compaction_tc_compacted_size")
            .help("Compacted topic size")
        )
      )

      compactedTopicErrors <- IO(
        cs.registerCounter(
          _.name("findify_pulsar_compaction_tc_errors")
            .help("Missing event errors")
        )
      )
      _ <- testLoop(
        url,
        namespace,
        topic,
        roundBreak,
        createDelete,
        compactedTopicSize,
        compactedTopicErrors
      ).foreverM

    } yield ()

  def testLoop(url: String,
               namespace: String,
               topic: String,
               roundBreak: Int,
               createDelete: Counter,
               compactedTopicSize: Gauge,
               compactedTopicErrors: Counter): IO[Unit] =
    for {
      pulsarClient <- IO(PulsarClient.builder().serviceUrl(url).build())
      producer     <- IO(PulsarInput(pulsarClient, namespace, topic))
      id1          <- IO(s"${System.currentTimeMillis()}-1")
      id2          <- IO(s"${System.currentTimeMillis()}-2")
      id3          <- IO(s"${System.currentTimeMillis()}-3")

      _ <- IO(println("Emitting updates events .."))
      _ <- PulsarInput.doEmit(producer, id1, Some(toByteArray(s"Some Product: ${id1}")))
      _ <- PulsarInput.doEmit(producer, id2, Some(toByteArray(s"Some Product: ${id2}")))
      _ <- PulsarInput.doEmit(producer, id3, Some(toByteArray(s"Some Product: ${id3}")))
      _ <- IO(createDelete.labels("create").inc(3))
      _ <- IO(println("Emitting delete events  .."))
      _ <- PulsarInput.doEmit(producer, id1, None)
      _ <- PulsarInput.doEmit(producer, id2, None)
      _ <- PulsarInput.doEmit(producer, id3, None)
      _ <- IO(createDelete.labels("delete").inc(3))
      v <- validateUniqueEventPresence(pulsarClient, namespace, topic, compactedTopicSize)
      _ <- if (v)
        IO.unit
      else {
        IO(println("----------------> Topic does not contains unique message"))
        IO(compactedTopicErrors.inc())
      }
      _ <- IO(producer.close())
      _ <- IO(pulsarClient.close())
      _ <- IO(println("----------------> Will wait for the next round"))
      _ <- IO.sleep((roundBreak).seconds)
    } yield ()

  def validateUniqueEventPresence(pulsarClient: PulsarClient,
                                  namespace: String,
                                  topic: String,
                                  compactedTopicSize: Gauge): IO[Boolean] =
    for {
      reader <- IO(PulsarOutput(pulsarClient, namespace, topic))
      existingKeys <- IO {
        var keys = List.empty[String]
        while (reader.hasNext) {
          keys = keys ++ reader.next().key
        }
        keys
      }
      _      <- IO(compactedTopicSize.set(existingKeys.size))
      status <- IO(existingKeys.contains(uniqueEvent))

    } yield (status)

}
