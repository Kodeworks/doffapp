package com.kodeworks.doffapp.actor

import akka.actor.{Cancellable, Actor, ActorLogging}
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitTimeout, InitFailure, InitSuccess}
import concurrent.duration._

/*
  * Boot service makes sure actors are initialized in the correct order -
  * that is, they dont ask each other stuff before their dependent actors are in the right state
  */
class BootService(val ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  var initTimeoutTimer: Cancellable = null

  override def preStart {
    log.info("Born => Db")
    ctx.dbService = service(new DbService(ctx), Some("dispatcher.db"))
    scheduleInitTimeoutTimer
  }

  override def receive = {
    case InitFailure =>
      log.error("Init failed, terminating")
      actorSystem.terminate
    case InitTimeout =>
      log.error("Init timed out (all expected successful init messages where not received in time), terminating")
      actorSystem.terminate
    case InitSuccess if serviceName[DbService] == sender.path.name =>
      log.info("Db => Tender")
      ctx.tenderService = service(new TenderService(ctx))
      scheduleInitTimeoutTimer
    case InitSuccess if serviceName[TenderService] == sender.path.name =>
      log.info("Tender = User")
      ctx.userService = service(new UserService(ctx))
      scheduleInitTimeoutTimer
    case InitSuccess if serviceName[UserService] == sender.path.name =>
      log.info("User => Classify")
      ctx.classifyService = service(new ClassifyService(ctx))
      scheduleInitTimeoutTimer
    case InitSuccess if serviceName[ClassifyService] == sender.path.name =>
      log.info("Classify => Crawl")
      ctx.crawlService = service(new CrawlService(ctx))
      scheduleInitTimeoutTimer
    case InitSuccess if serviceName[CrawlService] == sender.path.name =>
      log.info("Crawl => Http")
      ctx.httpService = service(new HttpService(ctx))
      scheduleInitTimeoutTimer
    case InitSuccess if serviceName[HttpService] == sender.path.name =>
      log.info("Http => init done, BootService terminating")
      context.stop(self)
    case x =>
      log.error("Unknown {}", x)
  }

  def scheduleInitTimeoutTimer {
    stopInitTimeoutTimer
    initTimeoutTimer = context.system.scheduler.scheduleOnce(bootInitTimeout millis, self, InitTimeout)
  }

  def stopInitTimeoutTimer {
    if (null != initTimeoutTimer) initTimeoutTimer.cancel
  }

  override def postStop {
    stopInitTimeoutTimer
  }
}
