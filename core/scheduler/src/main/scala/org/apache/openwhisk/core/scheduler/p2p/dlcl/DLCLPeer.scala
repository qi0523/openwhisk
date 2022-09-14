package org.apache.openwhisk.core.scheduler.p2p.dlcl

class DLCLPeer {

  private var ip: String = _

  private var srcIP: String = _

  var prePeer: DLCLPeer = _
  var nextPeer: DLCLPeer = _

  def this(ip: String) {
    this()
    this.ip = ip
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
