trait Node
class NodeImpl extends Node

class Tree[T <: NodeImpl] { // new version
  def contains(e: T): Boolean = true
}
