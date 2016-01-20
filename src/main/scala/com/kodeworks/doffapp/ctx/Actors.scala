package com.kodeworks.doffapp.ctx

import akka.actor.{ActorSystem, Props}
import com.kodeworks.doffapp.actors.{DbService, CrawlService, TenderService}

trait Actors {
  this: Ctx =>
  val actorSystem = ActorSystem(name)
  val dbService = actorSystem.actorOf(Props(new DbService(this)), classOf[DbService].getSimpleName)
  val tenderService = actorSystem.actorOf(Props(new TenderService(this)), classOf[TenderService].getSimpleName)
  val crawlService = actorSystem.actorOf(Props(new CrawlService(this)), classOf[CrawlService].getSimpleName)
}
