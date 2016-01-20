package com.kodeworks.doffapp.actors

import akka.actor.{Actor, ActorLogging}
import com.kodeworks.doffapp.actors.DbService.{Insert, Load, Loaded}
import com.kodeworks.doffapp.actors.TenderService._
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.model.Tender

import scala.collection.mutable

class TenderService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  val tenders = mutable.Map[String, Tender]()

  override def preStart {
    context.become(loading)
    dbService ! Load(classOf[Tender])
  }

  val loading: Receive = {
    case Loaded(data) =>
      println("Got data from db: ")
      tenders ++= data(classOf[Tender]).asInstanceOf[Seq[Tender]].map(t => t.doffinReference -> t)
      println(tenders.mkString("\n"))
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
