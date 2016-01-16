package com.kodeworks.doffapp.actors

import akka.actor.{Actor, ActorLogging}
import com.kodeworks.doffapp.actors.DbService.{Loaded, Load}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.model.Tender

import scala.collection.mutable

import concurrent.duration._

class TenderService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._
  import context.dispatcher

  val tenders = mutable.Map[String, Tender]()

  override def preStart {
    //    context.system.scheduler.scheduleOnce(20 seconds, dbService, Load(Tender))
    dbService ! Load(Tender)
  }

  override def receive = {
    case Loaded(data) =>
      println("Got data from db: ")
      val tenders = data(Tender).asInstanceOf[Seq[Tender]]
      println(tenders.mkString("\n"))
    case t: Tender =>
      if (tenders.contains(t.doffinReference)) {
        //        log.info("Tender received, but we already have it")
      } else {
        log.info("New tender received with reference {}", t.doffinReference)
        tenders += t.doffinReference -> t
        dbService ! t
      }
    case x =>
      log.error("Unknown " + x)
  }
}
