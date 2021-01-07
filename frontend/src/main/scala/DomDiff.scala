package eoi.frontend

import org.scalajs.dom._
import scala.scalajs.js.annotation._
import org.querki.jquery._

object DomDiff {
  implicit val bundle = scalatags.JsDom

  import bundle.all._

implicit class NodeListSeq[T <: Node](nodes: DOMList[T]) extends IndexedSeq[T] {
  override def foreach[U](f: T => U): Unit = {
    for (i <- 0 until nodes.length) {
      f(nodes(i))
    }
  }

  override def length: Int = nodes.length

  override def apply(idx: Int): T = nodes(idx)
}

  def applyDifferences(source: Element, destination: JQuery): Unit = {

    def inner(src: List[Node], dest: List[Node], destNode: Node): Unit = {
      (src, dest) match {
        case (Nil, Nil) => ()
        case (s::sx, Nil) => 
          destNode.appendChild(s)
          inner(sx, dest, destNode)
        case (Nil, d::dx) =>
          destNode.removeChild(d)
          inner(src, dx, destNode)
        case (s::sx,d::dx) if s.nodeName == d.nodeName && s.attributes == d.attributes =>
          inner(s.childNodes.toList, d.childNodes.toList, d)
          d.innerText = s.innerText
          inner(sx, dx, destNode)
        case (s::sx,d::dx) =>
          destNode.removeChild(d)          
          destNode.appendChild(s)
          inner(sx, dx, destNode)
      }
    }

    val destNode = destination.toArray().toList.head
    inner(
      source.childNodes.toList,
      destNode.childNodes.toList,
      destNode
    )
  }

}
