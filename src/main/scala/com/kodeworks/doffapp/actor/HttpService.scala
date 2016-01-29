package com.kodeworks.doffapp.actor

import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation.RouteWithConcatenation
import akka.http.scaladsl.server._
import akka.pattern.pipe
import akka.pattern.ask

import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess}
import HttpService._
import akka.http.scaladsl.server.Directives._
import com.kodeworks.doffapp.util.RichFuture
import scala.util.{Success, Failure}

class HttpService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  implicit val to = ctx.timeout

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

  override def receive = {
    case rc: RequestContext =>
      log.info("Called with requestcontext, forwarding, sender: {}", sender)
      route(rc).pipeTo(sender)
    case x =>
      log.error("Unknown " + x)
  }

  val route: Route = userService ~ tenderService
}

object HttpService {
  implicit def actor2Route(a: ActorRef)(implicit to: Timeout): StandardRoute =
    StandardRoute(rc => (a ? rc).mapTo[RouteResult])

  implicit def actor2RouteConcat[A1](a: A1)(implicit ev: A1 => StandardRoute, to: Timeout): RouteWithConcatenation = new RouteWithConcatenation(a)
}
