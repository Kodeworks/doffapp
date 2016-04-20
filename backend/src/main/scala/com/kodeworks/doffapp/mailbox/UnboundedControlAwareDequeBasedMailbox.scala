package com.kodeworks.doffapp.mailbox

import java.util
import java.util.Comparator

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch._

class UnboundedStablePriorityDequeBasedMailbox(val cmp: Comparator[Envelope], val initialCapacity: Int)
  extends MailboxType with ProducesMessageQueue[UnboundedStablePriorityDequeBasedMailbox.MessageQueue] {
  def this(cmp: Comparator[Envelope]) = this(cmp, 11)

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new UnboundedStablePriorityDequeBasedMailbox.MessageQueue(initialCapacity, cmp)
}

object UnboundedStablePriorityDequeBasedMailbox {

  class MessageQueue(initialCapacity: Int, cmp: Comparator[Envelope])
    extends StablePriorityBlockingDeque[Envelope](initialCapacity, cmp) with UnboundedDequeBasedMessageQueue {
    final def queue: util.Deque[Envelope] = this
  }

}