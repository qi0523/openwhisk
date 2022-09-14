package org.apache.openwhisk.core.scheduler.p2p

class NoneManagerProvider extends P2PManagerProvider {
  override def instance(): P2PManager = null
}
