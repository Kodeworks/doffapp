package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.actor.DbService.{Inserted, Insert, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess, SaveClassifys}
import com.kodeworks.doffapp.model.{Classify, Tender}

import scala.collection.mutable

class ClassifyService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  val classifys = mutable.Set[Classify]()

  override def preStart {
    context.become(initing)
    dbService ! Load(classOf[Classify])
  }

  val initing: Receive = {
    case Loaded(data, errors) =>
      if (errors.nonEmpty) {
        log.error("Critical databases error. Error loading data during boot.")
        bootService ! InitFailure
      } else {
        classifys ++= data(classOf[Classify]).asInstanceOf[Seq[Classify]]
        log.info("Loaded {} classifys", classifys.size)
        bootService ! InitSuccess
        context.unbecome
      }
    case x =>
      log.error("Loading - unknown message" + x)
  }

  override def receive = {
    case SaveClassifys(cs) =>
      val newClassifys = cs.filter(c => !classifys.contains(c))
      log.info("Got {} classifys, of which {} were new", cs.size, newClassifys.size)
      classifys ++= newClassifys
      if (newClassifys.nonEmpty) dbService ! Insert(newClassifys: _*)
    case Inserted(data, errors) =>
      log.info("Inserted classifys: {}", data)
      classifys ++= data.asInstanceOf[List[Classify]]
    case x => log.error("Unknown " + x)
  }
}

object ClassifyService {
}
