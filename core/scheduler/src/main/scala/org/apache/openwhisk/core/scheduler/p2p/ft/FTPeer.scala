package org.apache.openwhisk.core.scheduler.p2p.ft

class FTPeer{

  private var ip: String = _

  private var srcIP: String = _

  var id: Int = _
  var height: Int = 1
  var parent: FTPeer = _
  var leftChild: FTPeer = _
  var rightChild: FTPeer = _
  var ready: Boolean = _

  def this(ip: String) {
    this()
    this.ip = ip
  }

  def getHeight(): Int = {
    if (this == null) {
      return -1
    }
    this.height
  }

  def getBalanceFactor(): Int = {
    if (this == null) {
      return 0
    }
    this.leftChild.getHeight() - this.rightChild.getHeight()
  }

  def setIp(ip: String): Unit = {
    this.ip = ip
  }

  def getIp(): String = {
    this.ip
  }

  def setSrcIp(ip: String): Unit = {
    this.srcIP = ip
  }

  def getSrcIp(): String = {
    this.srcIP
  }

}
