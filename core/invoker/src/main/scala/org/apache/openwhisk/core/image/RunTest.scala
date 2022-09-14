package org.apache.openwhisk.core.image

import scala.collection.mutable.Map

class RunTest extends Runnable {

  private val m: Map[String, String] = Map.empty

  def insertM(key:String, value : String): Unit = {
    m.put(key, value)
  }

  override def run(): Unit = {
    println("hello world!")
    m.foreach(kv => {
      println(kv._1, " ", kv._2)
    })
  }
}
