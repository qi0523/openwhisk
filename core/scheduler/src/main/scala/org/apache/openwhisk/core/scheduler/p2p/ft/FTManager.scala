package org.apache.openwhisk.core.scheduler.p2p.ft

import org.apache.openwhisk.core.scheduler.p2p.P2PManager

import scala.collection.concurrent.TrieMap

class FTManager extends P2PManager {

  private val peerTable = TrieMap[String, FTPeer]()

  private val structureTable = TrieMap[String, FTStructure]()

  override def getSrcNode(coldCreationKey: String, hostIP: String): String = {
    val nodePeer = peerTable.get(hostIP + coldCreationKey)
    if (!nodePeer.isEmpty){
      return prefix + nodePeer.get.getSrcIp() // not need to pull image
    }

    val peer = new FTPeer(hostIP)

    peerTable.put(hostIP + coldCreationKey, peer)

    val ft = structureTable.get(coldCreationKey)

    if (ft.isEmpty) {
      // pull image from remote registry
      peer.setSrcIp(remoteRegistryUrl)
      val ftStructure = new FTStructure(coldCreationKey)
      ftStructure.insert(peer)
      structureTable.put(coldCreationKey, ftStructure)
      remoteRegistryUrl
    } else {
      val srcPeer = ft.get.insert(peer)
      if (srcPeer == null) {
        peer.setSrcIp(remoteRegistryUrl)
        remoteRegistryUrl
      } else {
        peer.setSrcIp(srcPeer.getIp())
        srcPeer.getIp()
      }
    }
  }

  override def deletePeer(coldCreationKey: String, hostIP: String): Unit = {

    val peer = peerTable.get(hostIP + coldCreationKey)

    if (!peer.isEmpty) {
      peerTable.remove(hostIP + coldCreationKey)
      structureTable.get(coldCreationKey).get.delete(peer.get)
    }
  }
}
