package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import com.kodeworks.doffapp.actor.DbService.{Insert, Load, Loaded}
import com.kodeworks.doffapp.actor.TenderService._
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.InitSuccess
import com.kodeworks.doffapp.model.Tender

import scala.collection.mutable

class TenderService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  val tenders = mutable.Map[String, Tender]()

  override def preStart {
    context.become(initing)
    dbService ! Load(classOf[Tender])
  }

  val initing: Receive = {
    case Loaded(data) =>
      tenders ++= data(classOf[Tender]).asInstanceOf[Seq[Tender]].map(t => t.doffinReference -> t)
      log.info("Loaded {} tenders", tenders.size)
      bootService ! InitSuccess
      context.unbecome
    case x =>
      log.error("Loading - unknown message" + x)
  }

  override def receive = {
    case SaveTenders(ts) =>
      val newTenders = ts.filter(t => !tenders.contains(t.doffinReference))
      log.info("Got {} tenders, of which {} were new", ts.size, newTenders.size)
      newTenders.foreach { t =>
        tenders += t.doffinReference -> t
        dbService ! Insert(t)
      }
    case x =>
      log.error("Unknown " + x)
  }
}

object TenderService {

  case class SaveTenders(tenders: Seq[Tender])

}
