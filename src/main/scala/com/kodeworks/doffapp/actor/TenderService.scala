package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.actor.DbService.{Insert, Load, Loaded}
import com.kodeworks.doffapp.actor.TenderService._
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess}
import com.kodeworks.doffapp.model.Tender
import concurrent.duration._
import scala.collection.mutable

class TenderService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  val tenders = mutable.Map[String, Tender]()

  override def preStart {
    context.become(initing)
    context.system.scheduler.scheduleOnce(10 seconds, dbService, Load(classOf[Tender]))
    //    dbService ! Load(classOf[Tender])
  }

  val initing: Receive = {
    case Loaded(data, errors) =>
      if (errors.nonEmpty) {
        log.error("Critical database error. Error loading data during boot.")
        bootService ! InitFailure
      } else {
        tenders ++= data(classOf[Tender]).asInstanceOf[Seq[Tender]].map(t => t.doffinReference -> t)
        log.info("Loaded {} tenders", tenders.size)
        bootService ! InitSuccess
        context.unbecome
      }
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
