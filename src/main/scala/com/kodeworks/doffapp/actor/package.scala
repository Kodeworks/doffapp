package com.kodeworks.doffapp

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorSystem, Actor}

import scala.reflect.ClassTag

package object actor {
  def serviceName[A <: Actor : ClassTag] =
    reflect.classTag[A].runtimeClass.getSimpleName

  def service[A <: Actor : ClassTag](a: => A, dispatcher: Option[String] = None)(implicit ac: ActorSystem) =
    ac.actorOf(
      if (dispatcher.isEmpty)
        Props(a)
      else Props(a).withDispatcher(dispatcher.get)
      , serviceName[A])

  def extractMessage(msgToReceive: Any => Receive): Receive = {
    var msg: Any = null
    new Receive {
      def isDefinedAt(x: Any) = {
        msg = x
        false
      }

      def apply(x: Any) {
        ???
      }
    } orElse msgToReceive(msg)
  }

}
