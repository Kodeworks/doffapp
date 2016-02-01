package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.actor.DbService.{Inserted, Insert, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess, SaveClassifys}
import com.kodeworks.doffapp.model.{Classify}
import akka.pattern.pipe
import scala.collection.mutable
import akka.http.scaladsl.marshallers.argonaut.ArgonautSupport._
import Classify.Json._

import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._

class ClassifyService(ctx: Ctx) extends Actor with ActorLogging {
  //TODO import Implicits for these imports/implicits
  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  implicit def sm = sessionManager

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

  val route = pathPrefix("classify") {
    (requiredSession(oneOff, usingCookies) & get) { user =>
      complete(classifys.filter(_.user == user))
    } ~ (requiredSession(oneOff, usingCookies) & post & path(Segment / IntNumber)) { (user, tenderDoffinReference, classifyNumber) =>
      validate(0 == classifyNumber || 1 == classifyNumber, "Classify-number must be 0 (uninterresting) or 1 (interresting)") {
        val classify = Classify(user, tenderDoffinReference, if(1 == classifyNumber) true else false)
        self ! SaveClassifys(Seq(classify))
        complete(classify)
      }
    }
  }

  override def receive = {
    case rc: RequestContext =>
      route(rc).pipeTo(sender)
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
