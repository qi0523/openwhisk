package org.apache.openwhisk.core.scheduler.p2p.ft

class FTStructure {

  val leftDir = 0
  val rightDir = 1
  val NULL = -1
  val maxLength = 20

  var key: String = _
  var root: FTPeer = _
  var length: Int = _

  def this(s: String) {
    this()
    this.key = s
  }

  def nextAvailableNode(): (FTPeer, Int, Boolean) = {
    var queue = List(this.root)
    var level = 1
    while (queue.size > 0) {
      if (level == maxLength) {
        return (null, NULL, false)
      }
      val l = queue.size
      for (i <- 0 to l) {
        val curPeer = queue(i)
        if (curPeer.leftChild == null) {
          return (curPeer, leftDir, true)
        } else {
          queue = queue :+ (curPeer.leftChild)
        }
        if (curPeer.rightChild == null) {
          return (curPeer, rightDir, true)
        } else {
          queue = queue :+ (curPeer.rightChild)
        }
      }
      level += 1
      queue = queue.drop(l)
    }
    (null, NULL, false)
  }

  def insert(appendNode: FTPeer): FTPeer = {
    this.synchronized {
      appendNode.id = this.length
      if (this.root == null) {
        println("insert to root")
        this.root = appendNode
        appendNode.parent = null
      } else {
        val (targetNode, direction, ok) = nextAvailableNode()
        if (!ok) {
          println("tree is full")
        } else {
          if (direction == leftDir) {
            targetNode.leftChild = appendNode
            appendNode.parent = targetNode
          } else {
            targetNode.rightChild = appendNode
            appendNode.parent = targetNode
          }
        }
      }
      var p = appendNode.parent
      while (p != null) {
        if (p.rightChild == null){
          p.height = 1 + p.leftChild.getHeight()
        }else{
          p.height = 1 + (p.leftChild.getHeight()).max(p.rightChild.getHeight())
        }
        p = p.parent
      }
      this.length += 1
      appendNode.parent
    }
  }

  def getSuccessor(p: FTPeer): FTPeer = {
    var queue = List(p)
    while (queue.size > 0) {
      val l = queue.size
      for (i <- 0 to l) {
        val curNode = queue(i)
        if (curNode.leftChild == null && curNode.rightChild == null) {
          return curNode
        }
        if (curNode.leftChild != null) {
          queue = queue :+ (curNode.leftChild)
        }
        if (curNode.rightChild != null) {
          queue = queue :+ (curNode.rightChild)
        }
      }
      queue = queue.drop(l)
    }
    null
  }

  def leftRotate(x: FTPeer) = {
    val y = x.rightChild
    x.rightChild = y.leftChild
    if (y.leftChild != null) {
      y.leftChild.parent = x
    }
    y.parent = x.parent

    if (x.parent == null) {
      this.root = y
    } else if (x == x.parent.leftChild) {
      x.parent.leftChild = y
    } else {
      x.parent.rightChild = y
    }
    y.leftChild = x
    x.parent = y

    x.height = 1 + (x.leftChild.getHeight()).max(x.rightChild.getHeight())
    y.height = 1 + (y.leftChild.getHeight()).max(y.rightChild.getHeight())
  }

  def rightRotate(x: FTPeer) = {
    val y = x.leftChild
    x.leftChild = y.rightChild

    if (y.rightChild != null) {
      y.rightChild.parent = x
    }
    y.parent = x.parent
    if (x.parent == null) {
      this.root = y
    } else if (x == x.parent.rightChild) {
      x.parent.rightChild = y
    } else {
      x.parent.leftChild = y
    }
    y.rightChild = x
    x.parent = y

    x.height = 1 + (x.leftChild.getHeight()).max(x.rightChild.getHeight())
    y.height = 1 + (y.leftChild.getHeight()).max(y.rightChild.getHeight())
  }

  def delete(deleteNode: FTPeer): Unit = {
    this.synchronized {
      if (deleteNode == null) return
      var parent = deleteNode.parent
      var root = this.root
      // Case 0: Delete Peer is root Peer and Root is the only Peer in this tree.
      if (deleteNode == root && root.leftChild == null && root.rightChild == null) {
        println("case0")
        this.root = null
        return
      }
      // Case 1: Delete Peer is leaf Peer, also need to check balance, if L & R have been deleted together
      //          1
      //         / \
      //        2  9
      //       /\  /
      //      3 4 10    Delete Peer 10 will incur in-balancing
      //     /\ /\
      //    5 6 7 8
      if (deleteNode != root && deleteNode.leftChild == null && deleteNode.rightChild == null) {
        println("case1")
        if (parent.leftChild == deleteNode) {
          parent.leftChild = null
        } else {
          parent.rightChild = null
        }
      }

      // Case 2.1 & 2.2:
      // Delete Peer only has left child, transplant
      // Delete Peer only has right child, transplant
      if (deleteNode.leftChild != null && deleteNode.rightChild == null) {
        println("Case2.1")
        if (deleteNode == root) {
          this.root = deleteNode.leftChild
          return
        } else {
          val l = deleteNode.leftChild
          l.parent = parent
          if (deleteNode == parent.leftChild) {
            parent.leftChild = l
          } else if (deleteNode == parent.rightChild) {
            parent.rightChild = l
          }
        }
      } else if (deleteNode.leftChild == null && deleteNode.rightChild != null) {
        println("Case2.2")
        if (deleteNode == root) {
          this.root = deleteNode.rightChild
          return
        } else {
          val r = deleteNode.rightChild
          r.parent = parent
          if (deleteNode == parent.leftChild) {
            parent.leftChild = r
          } else if (deleteNode == parent.rightChild) {
            parent.rightChild = r
          }
        }
      }

      // Case 3: Delete Peer has left and right child
      //            1
      //          /   \
      //         2     9
      //       /  \    / \
      //      3    4  10  11   Delete Peer 9 will incur partial in-balancing
      //     / \  / \     / \
      //    5  6  7  8   15  12
      //   / \
      //  13 14
      if (deleteNode.leftChild != null && deleteNode.rightChild != null) {
        println("Case3")
        var (l, r) = (deleteNode.leftChild, deleteNode.rightChild)
        val s = this.getSuccessor(deleteNode)
        val tmpParent = s.parent

        // Cut successor from its parent
        if (s == s.parent.leftChild) {
          s.parent.leftChild = null
        } else if (s == s.parent.rightChild) {
          s.parent.rightChild = null
        }

        // If s is the direct child of deleteNode
        if (s == l) {
          l = null
        } else if (s == r) {
          r = null
        }

        // Update connection between successor and parent
        // If delete Peer is root, set its parent to nil
        if (deleteNode == root) {
          s.parent = null
        } else {
          if (deleteNode == deleteNode.parent.leftChild) {
            deleteNode.parent.leftChild = s
          } else {
            deleteNode.parent.rightChild = s
          }
          s.parent = deleteNode.parent
        }

        // Update connection between successor and child
        s.leftChild = l
        if (l != null) {
          l.parent = s
        }
        s.rightChild = r
        if (r != null) {
          r.parent = s
        }

        // Update correct parent for checking balance factor
        if (tmpParent == deleteNode) {
          parent = s
        } else {
          parent = tmpParent
        }

        // If delete Peer is root, set new root to the tree
        if (deleteNode == root) {
          this.root = s
        }
      }

      while (parent != null) {
        parent.height = 1 + (parent.leftChild.getHeight()).max(parent.rightChild.getHeight())
        if (parent.getBalanceFactor() > 1 && parent.leftChild.getBalanceFactor() >= 0) {
          println("R")
          this.rightRotate(parent)
        } else if (parent.getBalanceFactor() < -1 && parent.rightChild.getBalanceFactor() <= 0) {
          println("L")
          this.leftRotate(parent)
        } else if (parent.getBalanceFactor() > 1 && parent.leftChild.getBalanceFactor() < 0) {
          println("LR")
          this.leftRotate(parent.leftChild)
          this.rightRotate(parent)
        } else if (parent.getBalanceFactor() < -1 && parent.rightChild.getBalanceFactor() > 0) {
          println("RL")
          this.rightRotate(parent.rightChild)
          this.leftRotate(parent)
        }
        parent = parent.parent
      }
    }
  }
}
