package org.apache.openwhisk.core.scheduler

import akka.actor.{ActorSystem, Props}
import org.apache.openwhisk.common.{GracefulShutdown, Logging}
import org.apache.openwhisk.core.WhiskConfig
import org.apache.openwhisk.core.connector.{InvokerGCMessage, MessageFeed, MessagingProvider}
import org.apache.openwhisk.core.entity.SchedulerInstanceId
import org.apache.openwhisk.core.scheduler.p2p.P2PManager
import org.apache.openwhisk.core.scheduler.p2p.ft.FTManager

import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ImageGCMessageConsumer(p2pManager: P2PManager,
                             schedulerInstanceId: SchedulerInstanceId,
                             config: WhiskConfig,
                             msgProvider: MessagingProvider)
                            (implicit actorSystem: ActorSystem, ec: ExecutionContext, logging: Logging) {
  private val topic = s"${Scheduler.topicPrefix}invokerGCImage${schedulerInstanceId.asString}"
  private val maxPeekPerPoll = 128
  private val consumer = msgProvider.getConsumer(config, topic, topic, maxPeekPerPoll)

  private def handler(bytes: Array[Byte]): Future[Unit] = Future {
    val raw = new String(bytes, StandardCharsets.UTF_8)
    InvokerGCMessage.parse(raw) match {
      case Success(msg) => {
        if (p2pManager != null) {
          if (p2pManager.isInstanceOf[FTManager]) {
            p2pManager.deletePeer(msg.actionName, msg.invokerIP)
          } else if (msg.kind == "blackbox") { // custom container, 使用容器名
            p2pManager.deletePeer(msg.actionName, msg.invokerIP)
          } else {
            p2pManager.deletePeer(msg.kind, msg.invokerIP)
          }
        }
        feed ! MessageFeed.Processed
      }
      case Failure(exception) =>
        logging.error(this, s"Failed to parse $bytes, error: ${exception.getMessage}")
        feed ! MessageFeed.Processed
      case _ =>
        logging.error(this, s"Unexpected message received $raw")
        feed ! MessageFeed.Processed
    }
  }

  private val feed = actorSystem.actorOf(Props {
    new MessageFeed("Image GC", logging, consumer, maxPeekPerPoll, 1.second, handler)
  })

  def close(): Unit = {
    feed ! GracefulShutdown
  }
}
