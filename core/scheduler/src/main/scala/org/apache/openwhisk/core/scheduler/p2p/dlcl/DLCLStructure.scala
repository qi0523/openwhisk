package org.apache.openwhisk.core.scheduler.p2p.dlcl

class DLCLStructure {

  var key: String = _

  val defaultCurrentNum = 2

  var srcPeer: DLCLPeer = _

  var tailPeer: DLCLPeer = _

  var reminderNum: Int = _

  def this(s: String) {
    this()
    this.key = s
  }

  def insert(appendNode: DLCLPeer): DLCLPeer = {
    this.synchronized {
      if (srcPeer == null) { // the first one
        appendNode.prePeer = appendNode
        appendNode.nextPeer = appendNode
        this.reminderNum = defaultCurrentNum
        srcPeer = appendNode
        tailPeer = appendNode
        null
      } else {
        appendNode.prePeer = tailPeer
        appendNode.nextPeer = tailPeer.nextPeer
        tailPeer.nextPeer.prePeer = appendNode
        tailPeer.nextPeer = appendNode
        tailPeer = appendNode
        if (this.reminderNum > 0) {
          this.reminderNum -= 1
        } else {
          srcPeer = srcPeer.nextPeer
          this.reminderNum = defaultCurrentNum - 1
        }
        srcPeer
      }
    }
  }


  def delete(deleteNode: DLCLPeer): Unit = {
    this.synchronized {
      if (deleteNode == null) return
      if (srcPeer == tailPeer && deleteNode == srcPeer) { //case1: one peer
        srcPeer = null
        tailPeer = null
        return
      }
      if (deleteNode == tailPeer) {
        tailPeer = deleteNode.prePeer
      }
      if (deleteNode == srcPeer) {
        srcPeer = srcPeer.nextPeer
        this.reminderNum = defaultCurrentNum
      }
      deleteNode.prePeer.nextPeer = deleteNode.nextPeer
      deleteNode.nextPeer.prePeer = deleteNode.prePeer
    }
  }
}
