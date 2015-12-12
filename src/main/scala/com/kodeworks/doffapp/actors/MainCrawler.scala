package com.kodeworks.doffapp.actors

import java.time.{Instant, ZoneOffset}

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.kodeworks.doffapp.actors.MainCrawler._
import com.kodeworks.doffapp.ctx.Ctx
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.Future

class MainCrawler(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  override def preStart {
    log.info("born")
    crawl
  }

  override def receive = {
    case Crawl => crawl
    case x => log.info("unknown message")
  }

  def crawl = {
    index
      .flatMap(_ => externalLogin)
      .flatMap(_ => internalLogin)
      .flatMap(_ => list)
      .onComplete { case doc =>
        doc.foreach(log.info("DOCUMENT\n{}", _))
        context.system.scheduler.scheduleOnce(crawlInterval, self, Crawl)
      }
  }

  def index: Future[Document] =
    doc(Http().singleRequest(HttpRequest(uri = mainUrl)))

  def externalLogin: Future[Document] =
    login(externalLoginUrl)

  def internalLogin: Future[Document] =
    login(internalLoginUrl)

  def doc(response: Future[HttpResponse]) =
    response
      .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
        .map(_.compact.utf8String))
      .map(Jsoup.parse _)

  def login(url: String) =
    doc(
      Marshal(FormData(Map(
        usernameKey -> loginUsername,
        passwordKey -> loginPassword
      ))).to[RequestEntity]
        .flatMap { case entity =>
          Http().singleRequest(HttpRequest(
            uri = url,
            method = POST,
            entity = entity))
        }
    )

  def list: Future[Document] =
    doc(Http().singleRequest(HttpRequest(
      uri = listUrl.format(listDateFormat.format(Instant.now.minusMillis(listBeforeNow).atOffset(ZoneOffset.UTC))))))
}

object MainCrawler {
  val passwordKey = "password"
  val usernameKey = "username"

  case object Crawl

}