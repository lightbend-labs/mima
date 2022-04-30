trait Node
class NodeImpl extends Node

class Tree[T <: Node] { // new version
  def contains(e: T): Boolean = true
}
