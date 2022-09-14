package org.apache.openwhisk.core.scheduler.p2p

import scala.util.control.Breaks

object Test {

  def main(args: Array[String]): Unit = {
    println("Hello, world!")
    println(sys.env("HOME"))

//    val p2p: P2PManager = new FTManager()
//
//    println(p2p.getSrcNode("nodejs12","192.168.1.1"))
//
//    println(p2p.getSrcNode("nodejs12","192.168.1.2"))
//
//    println(p2p.getSrcNode("nodejs12","192.168.1.3"))
//
//    println(p2p.getSrcNode("nodejs12","192.168.1.4"))
//
//    println(p2p.getSrcNode("nodejs12","192.168.1.5"))

//    val fqn = FullyQualifiedEntityName(EntityPath("namespace"), EntityName("entity"))
//    println(fqn)
//    println(fqn.toString)
//    println(fqn.name.toString)
//    println(System.currentTimeMillis())
//    println(System.nanoTime())

//    val s = "402a30fd96c17b2c6aaf4ccaf62bf2bff7192a2602e9e28a163fd8f35cdb2a7d mongo\n987038f006ad4ecec8e935ded6597007aba5d94170c708543c96323669aa8a11 consul:latest"
//    s.linesIterator.toSeq.foreach(a => println(a))
    //total: 210 304 475 136
    //free: 755 390 136 32
    //
//    val file = new File("/")
//    println("total: " + file.getTotalSpace)
//    println("free: " + file.getFreeSpace)
//    println("used: " + file.getUsableSpace * 100 / file.getTotalSpace)

//    val imagesInUse: Set[String] = Set.empty
//    imagesInUse.add("1")
//    imagesInUse.add("2")
//    imagesInUse.add("1")
//    imagesInUse.foreach(s => println(s))

//    val m: Map[String, Int] = Map.empty
//    m.put("1", 0)
    val loop = new Breaks
    val list = List(1,2,3,4,5)
    var sum = 0
    loop.breakable {
      for (i <- 0 until(list.size)) {
        println(list(i))
        sum += list(i)
        if (sum > 20) {
          loop.break()
        }
      }
    }
  }
}
