package org.apache.openwhisk.core.scheduler.p2p

trait P2PManager {

  val remoteRegistryUrl = "remote.registry.url"

  val prefix = "!@#$%^&*()"

  def getSrcNode(coldCreationKey: String, hostIP: String): String

  def deletePeer(coldCreationKey: String, hostIP: String)
}
