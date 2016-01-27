package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess}

import scala.concurrent.Future
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
    //TODO support up down if port is taken
    Http().bindAndHandle(route, httpInterface, httpPort).onComplete {
      case Success(ok) =>
        log.info("Bound to {}:{}", httpInterface, httpPort)
        bootService ! InitSuccess
        context.unbecome
      case Failure(no) =>
        log.error("Could not bind to {}:{} because of: {}", httpInterface, httpPort, no.getMessage)
        bootService ! InitFailure
    }
  }

  val initing: Receive = {
    case x =>
      log.error("Initing - unknown message " + x)
  }

  override def receive = {
    case x =>
      log.error("Unknown " + x)
  }

  val route = (rc: RequestContext) => (httpService ? rc).mapTo[RouteResult]
}
