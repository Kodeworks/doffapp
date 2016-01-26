package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.actor.DbService.{Insert, Inserted, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess, SaveTenders}
import com.kodeworks.doffapp.model.Tender

import scala.collection.mutable

class TenderService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  val tenders = mutable.Map[String, Tender]()

  override def preStart {
    context.become(initing)
    dbService ! Load(classOf[Tender])
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
      tenders ++= newTenders.map(t => t.doffinReference -> t)
      if (newTenders.nonEmpty) dbService ! Insert(newTenders: _*)
    case Inserted(data, errors) =>
      log.info("Inserted tenders: {}", data)
      tenders ++= data.asInstanceOf[List[Tender]].map(t => t.doffinReference -> t)
    case x =>
      log.error("Unknown " + x)
  }
}
