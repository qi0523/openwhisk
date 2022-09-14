package org.apache.openwhisk.core.scheduler.p2p.dlcl

import org.apache.openwhisk.core.scheduler.p2p.P2PManager

import scala.collection.concurrent.TrieMap

class DLCLManager extends P2PManager {

  private val peerTable = TrieMap[String, DLCLPeer]()

  private val structureTable = TrieMap[String, DLCLStructure]()

  override def getSrcNode(coldCreationKey: String, hostIP: String): String = {
    val nodePeer = peerTable.get(hostIP + coldCreationKey)
    if (!nodePeer.isEmpty){
      return prefix + nodePeer.get.getSrcIp() // not need to pull image
    }

    val peer = new DLCLPeer(hostIP)

    peerTable.put(hostIP + coldCreationKey, peer)

    val dlcl = structureTable.get(coldCreationKey)

    if (dlcl.isEmpty){
      // pull image from remote registry
      peer.setSrcIp(remoteRegistryUrl)
      val dlclStructure = new DLCLStructure(coldCreationKey)
      dlclStructure.insert(peer)
      structureTable.put(coldCreationKey, dlclStructure)
      remoteRegistryUrl
    }else{
      val srcPeer = dlcl.get.insert(peer)
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
