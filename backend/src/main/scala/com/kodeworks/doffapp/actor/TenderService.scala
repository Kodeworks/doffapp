package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.marshallers.argonaut.ArgonautSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.pattern.{ask, pipe}
import akka.stream.ActorMaterializer
import argonaut.Argonaut._
import argonaut.Shapeless._
import argonaut._
import com.kodeworks.doffapp.IdGen
import com.kodeworks.doffapp.actor.DbService.{Insert, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message._
import com.kodeworks.doffapp.model.Tender
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import shapeless.{::, HNil}

import scala.collection.mutable
import scala.reflect.runtime.universe.TypeTag

class TenderService
(ctx: Ctx)
(implicit val ltag: TypeTag[Tender :: HNil])
  extends Actor with ActorLogging with IdGen {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  implicit val to = timeout

  implicit def sm = sessionManager

  val tenders = mutable.Map[String, Tender]()
  val crawling = mutable.Map[String, Tender]()

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

  val route =
    (pathPrefix("tender") & requiredSession(oneOff, usingCookies)) { user =>
      get {
        path("relevant") {
          rc => (classifyService ? GetClassifications(user)).mapTo[GetClassificationsReply].flatMap {
            case GetClassificationsReply(Some(cs)) =>
              rc.complete(cs.map(c => tenders(c.tender) -> c))
            case _ => rc.complete("No classifications")
          }
        } ~
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
    case NewTenders(ts) =>
      val newTenders0 = ts.filter(t =>
        !tenders.contains(t.doffinReference)
          && !crawling.contains(t.doffinReference))
      log.info("Got {} tenders, of which {} were new", ts.size, newTenders0.size)
      newTenders(newTenders0)
      if (newTenders0.nonEmpty) dbService ! Insert(newTenders0: _*)
    case x =>
      log.error("Unknown " + x)
  }

  def newTenders(ts: Seq[Tender]) {
    //    crawling ++= ts.map(t => t.doffinReference -> t)

    tenders ++= ts.map(t => t.doffinReference -> t)
    tendersListener.foreach(_ ! ListenTendersReply(ts))
  }
}
