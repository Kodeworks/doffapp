package com.kodeworks.doffapp.ctx

import akka.actor.{ActorRef, ActorSystem, Props}
import com.kodeworks.doffapp.actor
import com.kodeworks.doffapp.actor.{BootService, DbService, CrawlService, TenderService}
import actor._

trait Actors {
  this: Ctx =>
  println("Loading Actors")
  val actorSystem = ActorSystem(name)
  val bootService: ActorRef = actorSystem.actorOf(Props(new BootService(this)), serviceName[BootService])
  var dbService: ActorRef = null
  var tenderService: ActorRef = null
  var crawlService: ActorRef = null
}
