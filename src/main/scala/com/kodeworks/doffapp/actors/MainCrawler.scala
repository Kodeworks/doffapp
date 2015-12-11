package com.kodeworks.doffapp.actors

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.ctx.Ctx

import scala.concurrent.Future

class MainCrawler(ctx: Ctx) extends Actor with ActorLogging {
  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  override def preStart {
    log.info("born")
    crawl(ctx.targetUrl)
  }

  override def receive = {
    case x => log.info("unknown message")
  }

  def crawl(targetUrl: String) = {
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = targetUrl))
    responseFuture.onSuccess {
      case s => println(s)
    }
  }
}
