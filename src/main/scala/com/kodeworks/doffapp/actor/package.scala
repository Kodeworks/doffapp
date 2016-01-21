package com.kodeworks.doffapp

import akka.actor.{Props, ActorSystem, Actor}

import scala.reflect.ClassTag

package object actor {
  def serviceName[A <: Actor : ClassTag] =
    reflect.classTag[A].runtimeClass.getSimpleName

  def service[A <: Actor:ClassTag](a: => A)(implicit ac: ActorSystem) =
    ac.actorOf(Props(a), serviceName[A])

}
