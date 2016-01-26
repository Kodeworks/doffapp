package com.kodeworks.doffapp.mailbox

import java.util
import java.util.concurrent.BlockingDeque
import java.util.concurrent.atomic.AtomicLong
import java.util.{Comparator, Iterator}

import com.kodeworks.doffapp.actor.PriorityBlockingDeque

/**
  * PriorityDequeStabilizer wraps a priority deque so that it respects FIFO for elements of equal priority.
  */
trait PriorityDequeStabilizer[E <: AnyRef] extends util.AbstractQueue[E] with util.Deque[E] {
  val backingDeque: BlockingDeque[PriorityDequeStabilizer.WrappedElement[E]]
  val seqNum = new AtomicLong(0)

  override def peek(): E = {
    val wrappedElement = backingDeque.peek()
    if (wrappedElement eq null) null.asInstanceOf[E] else wrappedElement.element
  }

  override def size(): Int = backingDeque.size()

  override def offer(e: E): Boolean = {
    if (e eq null) throw new NullPointerException
    val wrappedElement = new PriorityDequeStabilizer.WrappedElement(e, seqNum.incrementAndGet)
    backingDeque.offer(wrappedElement)
  }

  override def iterator(): Iterator[E] = new Iterator[E] {
    private[this] val backingIterator = backingDeque.iterator()

    def hasNext: Boolean = backingIterator.hasNext

    def next(): E = backingIterator.next().element

    def remove() = backingIterator.remove()
  }

  override def poll(): E = {
    val wrappedElement = backingDeque.poll()
    if (wrappedElement eq null) null.asInstanceOf[E] else wrappedElement.element
  }

  override def addFirst(e: E) {
    if (e eq null) throw new NullPointerException
    val wrappedElement = new PriorityDequeStabilizer.WrappedElement(e, seqNum.incrementAndGet)
    backingDeque.addFirst(wrappedElement)
  }

  override def getLast: E = ???

  override def pollLast(): E = ???

  override def offerFirst(e: E): Boolean = ???

  override def getFirst: E = ???

  override def removeFirst(): E = ???

  override def removeLastOccurrence(o: scala.Any): Boolean = ???

  override def push(e: E): Unit = ???

  override def addLast(e: E): Unit = ???

  override def pollFirst(): E = ???

  override def removeFirstOccurrence(o: scala.Any): Boolean = ???

  override def offerLast(e: E): Boolean = ???

  override def descendingIterator(): util.Iterator[E] = ???

  override def peekLast(): E = ???

  override def pop(): E = ???

  override def peekFirst(): E = ???

  override def removeLast(): E = ???

}

object PriorityDequeStabilizer {

  class WrappedElement[E](val element: E, val seqNum: Long)

  class WrappedElementComparator[E](val cmp: Comparator[E]) extends Comparator[WrappedElement[E]] {
    def compare(e1: WrappedElement[E], e2: WrappedElement[E]): Int = {
      val baseComparison = cmp.compare(e1.element, e2.element)
      if (baseComparison != 0) baseComparison
      else {
        val diff = e1.seqNum - e2.seqNum
        java.lang.Long.signum(diff)
      }
    }
  }

}

/**
  * StablePriorityBlockingDeque is a blocking priority deque that preserves order for elements of equal priority.
  *
  * @param capacity - the initial capacity of this Queue
  * @param cmp      - Comparator for comparing Deque elements
  */
class StablePriorityBlockingDeque[E <: AnyRef](capacity: Int, cmp: Comparator[E]) extends PriorityDequeStabilizer[E] {
  val backingDeque = new PriorityBlockingDeque[PriorityDequeStabilizer.WrappedElement[E]](
    new PriorityDequeStabilizer.WrappedElementComparator[E](cmp), capacity)
}
