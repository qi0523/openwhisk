package org.apache.openwhisk.core.image

import akka.actor.ActorSystem
import org.apache.openwhisk.utils.ExecutionContextFactory


object ImageClientTest {

  def main(args: Array[String]): Unit = {
    implicit val ec = ExecutionContextFactory.makeCachedThreadPoolExecutionContext()
    implicit val actorSystem: ActorSystem =
      ActorSystem(name = "invoker-actor-system", defaultExecutionContext = Some(ec))
//    val imageClient = new ImageClient(executionContext = actorSystem.dispatcher)
//    println("hello world!")
//    imageClient.ps(true).foreach(s => println(s))
//    Thread.sleep(5000)
//    actorSystem.terminate()

//    val testMap: mutable.Map[String, ImageRecord] = mutable.Map.empty
//    testMap += ("12" -> ImageRecord(12345))
//    println(testMap("12"))
    println("hello world")
    import scala.language.postfixOps
    import scala.concurrent.duration._
    val r = new RunTest()
    actorSystem.scheduler.scheduleWithFixedDelay(0 seconds, 2 seconds)(r)(actorSystem.dispatcher)
    Thread.sleep(3000)
    r.insertM("1","2")
    Thread.sleep(10000)
    actorSystem.terminate()
  }

}
