package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess}

/*
  * Boot service makes sure actors are initialized in the correct order -
  * that is, they dont ask each other stuff before their dependent actors are in the right state
  */
class BootService(val ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit def ac = actorSystem

  override def preStart {
    log.info("Born => Db")
    ctx.dbService = service(new DbService(ctx), Some("dispatcher.db"))
  }

  override def receive = {
    case InitFailure =>
      log.error("Init failed, terminating")
      actorSystem.terminate
    case InitSuccess if serviceName[DbService] == sender.path.name =>
      log.info("Db => Tender")
      ctx.tenderService = service(new TenderService(ctx))
    case InitSuccess if serviceName[TenderService] == sender.path.name =>
      log.info("Tender => Crawl")
      ctx.crawlService = service(new CrawlService(ctx))
    case InitSuccess if serviceName[CrawlService] == sender.path.name =>
      log.info("Crawl => init done, BootService terminating")
      context.stop(self)
    case x =>
      log.error("Unknown {}", x)
  }
}
