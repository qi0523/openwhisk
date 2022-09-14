package org.apache.openwhisk.core.scheduler.p2p.ft

import org.apache.openwhisk.core.scheduler.p2p.{P2PManager, P2PManagerProvider}

object FTManagerProvider extends P2PManagerProvider{
  override def instance(): P2PManager = new FTManager()
}
