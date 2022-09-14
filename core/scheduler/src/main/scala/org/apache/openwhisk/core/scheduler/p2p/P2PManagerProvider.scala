package org.apache.openwhisk.core.scheduler.p2p

import org.apache.openwhisk.spi.Spi

trait P2PManagerProvider extends Spi{
  def instance(): P2PManager
}
