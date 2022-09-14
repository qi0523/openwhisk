package org.apache.openwhisk.core.image

import akka.actor.ActorSystem
import org.apache.openwhisk.core.ConfigKeys
import org.apache.openwhisk.core.connector.InvokerGCMessage
import org.apache.openwhisk.core.entity.{InvokerInstanceId, SchedulerInstanceId, WhiskAction}

import scala.collection.mutable.{ListBuffer, Map, Set}
import pureconfig.loadConfigOrThrow
import pureconfig.generic.auto._

import java.io.File
import scala.concurrent.ExecutionContext
import scala.util.control.Breaks

case class DiskPath(path: String)

case class ImageGCPolicyConfig(HighThresholdPercent: Int, LowThresholdPercent: Int) {
  require(LowThresholdPercent >= 0 && LowThresholdPercent < HighThresholdPercent && HighThresholdPercent <= 100)
}

case class GCMetaData(sid: SchedulerInstanceId, action: WhiskAction)

// availableBytes: 可用的磁盘字节数
case class FsStats(var availableBytes: Long, capacityBytes: Long)

case class ImageRecord(imageRefs: Set[String], size: Long)

case class EvictionInfo(id: String, imageRecord: ImageRecord)

class ImageGCManager(instance: InvokerInstanceId,
                     sendAckToScheduler: (SchedulerInstanceId, InvokerGCMessage) => Unit,
                     policy: ImageGCPolicyConfig = loadConfigOrThrow[ImageGCPolicyConfig](ConfigKeys.imageGCConfig),
                     disk: DiskPath = loadConfigOrThrow[DiskPath](ConfigKeys.diskPath))
                    (implicit ec: ExecutionContext, as: ActorSystem) extends Runnable {

  //private val ec: ExecutionContext = as.dispatcher

  private val fs = new File(disk.path)

  private val imageRecords: Map[String, ImageRecord] = Map.empty

  private val schedulerGCMetaData: Map[String, GCMetaData] = Map.empty

  private val imageClient = new ImageClient(executionContext = ec)

  private def imageFsStats(): FsStats = {
    FsStats(fs.getUsableSpace, fs.getTotalSpace)
  }

  private def toBytes(size: String, unit: String): Long = {
    if (unit.contains('M')) {
      return (size.toDouble * 1024 * 1024).toLong
    }
    if (unit.contains('K')) {
      return (size.toDouble * 1024).toLong
    }
    if (unit.contains('G')) {
      return (size.toDouble * 1024 * 1024 * 1024).toLong
    }
    size.toLong
  }

  private def detectImages(): Set[String] = {

    val imagesInUse: Set[String] = Set.empty
    val imagesInProcess = imageClient.imageList()

    val containersInProcess = imageClient.ps(true)

    for {
      images <- imagesInProcess
      containers <- containersInProcess
    } yield {
      if(images.isEmpty || containers.isEmpty) {
        return imagesInUse
      }
      val containerAndImages: Map[String, String] = Map.empty
      this.synchronized {
        val currentImages: Set[String] = Set.empty //store sha256:2d3h
        images.foreach(im => {
          //val imId = im.repository + ":" + im.tag + "&&" + im.imageId
          containerAndImages.put(im.repository + ":" + im.tag, im.imageId)
          currentImages.add(im.imageId)
          if (!imageRecords.contains(im.imageId)) {
            imageRecords.put(im.imageId, ImageRecord(Set(im.repository + ":" + im.tag), toBytes(im.imageSize, im.imageUnit) + toBytes(im.blobSize, im.blobUnit)))
          } else {
            val imRefs = imageRecords.get(im.imageId).get.imageRefs
            if (!imRefs.contains(im.repository + ":" + im.tag)) {
              imRefs.add(im.repository + ":" + im.tag)
            }
          }
        })

        imageRecords.foreach(kv => {
          if (!currentImages.contains(kv._1)) {
            imageRecords.remove(kv._1)
          }
        })
      }
      // imageInUse   //containers.foreach(c => imagesInUse.add(c.imageId))
      containers.foreach(c => {
        imagesInUse.add(containerAndImages.get(c.imageId).get)
      })
    }
    imagesInUse
  }

  private def freeSpace(bytesToFree: Long): Long = {
    val imageInUse = detectImages()
    var spaceFreed = 0L
    this.synchronized {
      val evictionImage: ListBuffer[EvictionInfo] = ListBuffer.empty
      imageRecords.foreach(kv => {
        if (!imageInUse.contains(kv._1)) {
            evictionImage.append(EvictionInfo(kv._1, kv._2))
          }
      })
      val loop = new Breaks
      loop.breakable{
        for (i <- 0 until(evictionImage.size)) {
          val image = evictionImage(i)
          println("Removing image to free bytes", "imageID", image.id, "size", image.imageRecord.size)
          imageClient.rmi(image.id).map(_ => {
            imageRecords.remove(image.id)
            // tag 多份文件
            spaceFreed += image.imageRecord.size
            // send to scheduler, the invoker rm this image
            image.imageRecord.imageRefs.foreach(imageRef => {
              val metaData = schedulerGCMetaData.get(imageRef)
              if (!metaData.isEmpty){
                sendAckToScheduler(metaData.get.sid, InvokerGCMessage(metaData.get.action.name.toString, metaData.get.action.exec.kind, instance.hostIp))
              }
            })
          })
          if (spaceFreed >= bytesToFree) {
            loop.break()
          }
        }
      }
    }
    spaceFreed
  }

  private def imageGC(): Unit = {

    val fsStats = imageFsStats()

    if (fsStats.availableBytes > fsStats.capacityBytes) {
      fsStats.availableBytes = fsStats.capacityBytes
    }

    if (fsStats.capacityBytes == 0) {
      println("invalid capacity 0 on image filesystem")
      return
    }

    val usagePercent = 100 - fsStats.availableBytes * 100 / fsStats.capacityBytes

    if (usagePercent >= policy.HighThresholdPercent) {
      val amountToFree = fsStats.capacityBytes * (100 - policy.LowThresholdPercent) / 100 - fsStats.availableBytes
      val freed = freeSpace(amountToFree)
      if (freed < amountToFree) {
        println("failed to garbage collect required amount of images. Wanted to free " + amountToFree + " bytes, but freed " + freed + " bytes")
      }
    }
  }

  def insertMetaData(key: String, sid: SchedulerInstanceId, action: WhiskAction): Unit = {
    if (!schedulerGCMetaData.contains(key)) {
      schedulerGCMetaData.put(key, GCMetaData(sid, action))
    }
  }

  override def run(): Unit = {
    this.imageGC()
  }
}
