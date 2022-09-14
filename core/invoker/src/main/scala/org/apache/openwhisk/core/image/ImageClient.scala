package org.apache.openwhisk.core.image

import akka.actor.ActorSystem
import org.apache.openwhisk.core.ConfigKeys
import org.apache.openwhisk.core.containerpool.docker.ProcessRunner
import pureconfig.loadConfigOrThrow
import pureconfig.generic.auto._

import java.io.FileNotFoundException
import java.nio.file.{Files, Paths}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

case class ImageClientTimeoutConfig(imageList: Duration,
                                     rmi: Duration,
                                     ps: Duration)

case class ImageClientConfig(timeouts: ImageClientTimeoutConfig)

case class ContainerAndImage(containerId: String, imageId: String)

case class Image(repository: String, tag: String, imageId: String, imageSize: String, imageUnit: String, blobSize: String, blobUnit: String)

class ImageClient(config: ImageClientConfig = loadConfigOrThrow[ImageClientConfig](ConfigKeys.imageClient), executionContext: ExecutionContext)(implicit as: ActorSystem)
  extends ImageApi with ProcessRunner {

  implicit private val ec = executionContext

  private val dockerCmd: Seq[String] = {
    val alternatives = List("/usr/local/bin/nerdctl")
    //val alternatives = List("/usr/bin/docker")
    val dockerBin = Try {
      alternatives.find(a => Files.isExecutable(Paths.get(a))).get
    } getOrElse {
      throw new FileNotFoundException(s"Couldn't locate docker binary (tried: ${alternatives.mkString(", ")}).")
    }

    Seq(dockerBin)
  }

  /**
   * Returns a list of ContainerIds in the system.
   *
   * @param all Whether or not to return stopped containers as well
   * @return A list of ContainerIds
   */
  override def ps(all: Boolean): Future[Seq[ContainerAndImage]] = {
    val awk = Seq("awk", "NR>1{print $1,$2}")
    val allArg = if (all) Seq("--all") else Seq.empty[String]
    val cmd = (Seq("ps", "--no-trunc") ++ allArg)
    runCmdWithPipe(cmd, config.timeouts.ps, awk).map(_.linesIterator.toSeq.map(line => {
      val containerAndImage = line.split(" ")
      ContainerAndImage(containerAndImage(0), containerAndImage(1))
    }))
  }

  /**
   * Removes the image with the given id.
   *
   * @param imageId the id of the image to remove
   * @return a Future completing according to the command's exit-code
   */
  override def rmi(imageId: String): Future[Unit] = {
    runCmd(Seq("rmi", imageId), config.timeouts.rmi).map(_ => ())
  }

  // docker image ls --no-trunc | awk 'NR>0{print $3}'
  override def imageList(): Future[Seq[Image]] = {
    val awk = Seq("awk", "NR>1{print $1, $2, $3, $8, $9, $10, $11}")
    val cmd = Seq("image", "ls", "--all", "--no-trunc")
    runCmdWithPipe(cmd, config.timeouts.imageList, awk).map(_.linesIterator.toSeq.map(line => {
      val imInfo = line.split(" ")
      Image(imInfo(0), imInfo(1), imInfo(2), imInfo(3), imInfo(4), imInfo(5), imInfo(6))
    }))
  }

  protected def runCmd(args: Seq[String], timeout: Duration): Future[String] = {
    val cmd = dockerCmd ++ args
    executeProcess(cmd, timeout)
  }

  protected def runCmdWithPipe(args: Seq[String], timeout: Duration, awk: Seq[String]): Future[String] = {
    val cmd = dockerCmd ++ args
    executeProcessWithPipe(cmd, timeout, awk)
  }
}


trait ImageApi {

  /**
   * Returns a list of ContainerIds in the system.
   *
   * @param all Whether or not to return stopped containers as well
   * @return A list of ContainerIds
   */
  def ps(all: Boolean = false): Future[Seq[ContainerAndImage]]

  /**
   * Removes the container with the given id.
   *
   * @param imageId the id of the container to remove
   * @return a Future completing according to the command's exit-code
   */
  def rmi(imageId: String): Future[Unit]

  def imageList(): Future[Seq[Image]]

}