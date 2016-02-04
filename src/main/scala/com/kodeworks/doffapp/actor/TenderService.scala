package com.kodeworks.doffapp.actor

import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.actor.DbService.{Insert, Inserted, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message._
import com.kodeworks.doffapp.model.{User, Tender}
import com.kodeworks.doffapp.nlp.{CompoundSplitter, SpellingCorrector}
import akka.http.scaladsl.marshallers.argonaut.ArgonautSupport._
import Tender.Json._
import akka.pattern.pipe

import akka.pattern.ask
import scala.collection.mutable

class TenderService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  implicit val to = timeout

  val tenders = mutable.Map[String, Tender]()

  var tendersListener: Option[ActorRef] = None

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
        newTenders(data(classOf[Tender]).asInstanceOf[Seq[Tender]])
        log.info("Loaded {} tenders", tenders.size)
        bootService ! InitSuccess
        context.unbecome
      }
    case x =>
      log.error("Loading - unknown message" + x)
  }

  val route = pathPrefix("tender") {
    get {
      complete(tenders.map(_._2))
    }
  }

  override def receive = {
    case rc: RequestContext =>
      log.info("tenderservice rc, thread {}", Thread.currentThread().getId + " " + Thread.currentThread().getName)
      route(rc).pipeTo(sender)
    case ListenTenders =>
      tendersListener = Some(sender)
      sender ! ListenTendersReply(tenders.values.toSeq)
    case ListenTenders(ts) =>
      tendersListener = Some(sender)
      sender ! ListenTendersReply(tenders.filter(dt => ts.contains(dt._1)).values.toSeq)
    case SaveTenders(ts) =>
      val newTenders0 = ts.filter(t => !tenders.contains(t.doffinReference))
      log.info("Got {} tenders, of which {} were new", ts.size, newTenders0.size)
      newTenders(newTenders0)
      if (newTenders0.nonEmpty) dbService ! Insert(newTenders0: _*)
    case Inserted(data, errors) =>
      log.info("Inserted tenders: {}", data)
      tenders ++= data.asInstanceOf[Map[Tender, Option[Long]]].map {
        case (t, id) => t.doffinReference -> t.copy(id = id)
      }
    case x =>
      log.error("Unknown " + x)
  }

  def newTenders(ts: Seq[Tender]) {
    tenders ++= ts.map(t => t.doffinReference -> t)
    tendersListener.foreach(_ ! ListenTendersReply(ts))
  }
}
