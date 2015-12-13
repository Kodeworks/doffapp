package com.kodeworks.doffapp.actors

import java.time.{Instant, LocalDate, ZoneOffset}

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.kodeworks.doffapp.actors.MainCrawler._
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.model.Tender
import org.jsoup.Jsoup._
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConverters._
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

  def crawl =
    index
      .flatMap(_ => externalLogin)
      .flatMap(_ => internalLogin)
      .flatMap(_ => list)
      .flatMap(split _)
      .onComplete { case tenders =>
        tenders.foreach(tenders => log.info("DOCUMENT\n{}", tenders.mkString("\n")))
        context.system.scheduler.scheduleOnce(crawlInterval, self, Crawl)
      }

  def index: Future[Document] =
    doc(Http().singleRequest(HttpRequest(uri = mainUrl)))

  def doc(response: Future[HttpResponse]) =
    response
      .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
        .map(_.compact.utf8String))
      .map(parse _)

  def externalLogin: Future[Document] =
    login(loginExternalUrl)

  def internalLogin: Future[Document] =
    login(loginInternalUrl)

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

  def split(document: Document): Future[List[Tender]] = {
    Future(
      document.select("article.widget-content.widget-eps-search-result-widget.widget > div.notice-search-item").asScala.toList.map { element =>
        val nameInternalUriElem = element.select("div.notice-search-item-header > a").last
        //        try {
        val internalUrl = Path(nameInternalUriElem.attr("href"))
        val name = nameInternalUriElem.text()
        val flag = element.select("div.notice-search-item-header > img").first.attr("src").split("/").last.split("_").last.split("\\.").head
        val publishedByElem: Element = element.select("div.left-col > div:nth-child(1)").first
        val publishedBy = publishedByElem.text.stripPrefix("Publisert av: ")
        val publishedByUrl = Option(publishedByElem.children.first).map(elem => Uri(elem.attr("href").trim))
        val doffinReference = element.select("div.right-col > div:nth-child(1)").first.text.stripPrefix("Doffin referanse: ")
        val announcementType = element.select("div.left-col > div:nth-child(2)").text().stripPrefix("Kunngj\u00f8ringstype: ")
        val announcementDate = parseInstant(element.select("div.right-col > div:gt(0):last-child").text().stripPrefix("Kunngj\u00f8ringsdato: "))
        val tenderDeadline = Option(element.select("div.right-col > div:nth-child(2) > span").first).map(elem => parseInstant(elem.text))
        val county = Option(element.select("div:gt(3):lt(6):containsOwn(Kommune)").first).map(_.text.stripPrefix("Kommune: "))
        val municipality = Option(element.select("div:gt(3):lt(6):containsOwn(Fylke)").first).map(_.text.stripPrefix("Fylke: "))
        val externalUrl = Option(element.select("div.notice-search-item-header > a.pull-right.doc-icon").first).map(elem => Uri(elem.attr("href").trim))
        Tender(name, internalUrl, flag, publishedBy, publishedByUrl, doffinReference, announcementType, announcementDate, tenderDeadline, county, municipality, externalUrl)
        //        } catch {
        //          case e => {
        //            e.printStackTrace
        //            throw e
        //          }
        //        }
      }
    )
  }

  def parseInstant(date: String) = LocalDate.parse(date, listDateFormat).atStartOfDay.toInstant(ZoneOffset.UTC)
}

object MainCrawler {
  val passwordKey = "password"
  val usernameKey = "username"

  case object Crawl

}