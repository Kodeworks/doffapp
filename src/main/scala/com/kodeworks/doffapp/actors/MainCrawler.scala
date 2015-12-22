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
import com.kodeworks.doffapp.service.Brain
import org.jsoup.Jsoup._
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConverters._
import scala.collection.mutable
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
        tenders.foreach { tenders =>
          log.info("max words in \"name\" field: " + tenders.map(_.name.split(' ').size).max) //26
          log.info("max chars in \"name\" field: " + tenders.map(_.name.size).max) //182
          log.info("running muncipality == oslo test")
          var municipalityCount = 0
          val municipalityDictionary = mutable.Map[String, Int]()
          val municipalityTenders: List[Int] = tenders.map { t =>
            if (t.municipality.isEmpty) -1
            else {
              val municipality = t.municipality.get.toLowerCase
              municipalityDictionary.get(municipality) match {
                case Some(i) => i
                case _ =>
                  municipalityDictionary.put(municipality, municipalityCount)
                  municipalityCount += 1
                  municipalityCount - 1
              }
            }
          }
          val oslo = municipalityDictionary("oslo")
          val inputOutput = municipalityTenders.map {
            case `oslo` => List(oslo) -> List(1)
            case m => List(m) -> List(0)
          }
          Brain.train(inputOutput)
          val osloTest = Brain.run(List(oslo))
          log.info("osloTest: " + osloTest)
        }
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