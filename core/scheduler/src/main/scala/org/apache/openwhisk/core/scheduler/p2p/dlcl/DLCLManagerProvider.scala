package org.apache.openwhisk.core.scheduler.p2p.dlcl

import org.apache.openwhisk.core.scheduler.p2p.{P2PManager, P2PManagerProvider}

object DLCLManagerProvider extends P2PManagerProvider{
  override def instance(): P2PManager = new DLCLManager()
}
