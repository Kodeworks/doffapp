package com.kodeworks.doffapp.ctx

import akka.actor.{ActorRef, ActorSystem, Props}
import com.kodeworks.doffapp.actor
import com.kodeworks.doffapp.actor.{BootService, DbService, CrawlService, TenderService}
import actor._

trait Actors {
  val actorSystem: ActorSystem
  val bootService: ActorRef
  var dbService: ActorRef = null
  var tenderService: ActorRef = null
  var userService: ActorRef = null
  var crawlService: ActorRef = null
  var classifyService: ActorRef = null
  var httpService: ActorRef = null
}

trait ActorsImpl extends Actors {
  this: Ctx =>
  println("Loading Actors")
  override val actorSystem = ActorSystem(name)
  override val bootService: ActorRef = actorSystem.actorOf(Props(new BootService(this)), serviceName[BootService])
}
