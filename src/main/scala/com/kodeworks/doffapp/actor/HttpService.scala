package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation.RouteWithConcatenation
import akka.http.scaladsl.server._
import akka.pattern.{ask, pipe}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.kodeworks.doffapp.actor.HttpService._
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess}
import com.kodeworks.doffapp.util.RichFuture

import scala.util.{Failure, Success}

import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._

class HttpService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  implicit val to = timeout

  implicit def sm = sessionManager

  override def preStart {
    log.info("born")
    context.become(initing)
    bind
  }

  override def postStop {
    log.info("died")
  }

  def bind {
    Http().bindAndHandle(self: Route, httpInterface, httpPort).mapAll(t => t).pipeTo(self)
  }

  val initing: Receive = {
    case Success(ok) =>
      log.info("Bound to {}:{}", httpInterface, httpPort)
      bootService ! InitSuccess
      context.unbecome
    case Failure(no) =>
      log.error("Could not bind to {}:{} because of: {}", httpInterface, httpPort, no.getMessage)
      bootService ! InitFailure
    case x =>
      log.error("Initing - unknown message " + x)
  }

  val route: Route =
    userService ~
      touchRequiredSession(oneOff, usingCookies) { session =>
        tenderService ~
          classifyService
      }

  override def receive = {
    case rc: RequestContext =>
      log.info("Called with requestcontext, forwarding, sender: {}", sender)
      route(rc).pipeTo(sender)
    case x =>
      log.error("Unknown " + x)
  }

}

object HttpService {
  implicit def actor2Route(a: ActorRef)(implicit to: Timeout): StandardRoute =
    StandardRoute(rc => (a ? rc).mapTo[RouteResult])

  implicit def actor2RouteConcat[A1](a: A1)(implicit ev: A1 => StandardRoute, to: Timeout): RouteWithConcatenation = new RouteWithConcatenation(a)
}
