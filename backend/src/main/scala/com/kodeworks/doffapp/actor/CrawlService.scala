package com.kodeworks.doffapp.actor

import java.time._

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.kodeworks.doffapp.actor.CrawlService._
import com.kodeworks.doffapp.actor.DbService.{Load, Loaded, Upsert, Upserted}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess, NewTenders}
import com.kodeworks.doffapp.model.{CrawlData, Tender}
import org.jsoup.Jsoup
import com.kodeworks.doffapp.util.RichFuture
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Try

//TODO keep httpsession across one crawl
class CrawlService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  var last = CrawlData(Instant.now.minusMillis(listBeforeNow).atOffset(ZoneOffset.UTC).toInstant.toEpochMilli)
  var lastTender = ""

  override def preStart {
    log.info("born")
    context.become(initing)
    dbService ! Load(classOf[CrawlData])
  }

  val initing: Receive = {
    case Loaded(data, errors) =>
      log.info("Loaded {}", data)
      if (errors.nonEmpty) {
        log.error("Critical database error. Error loading data during boot.")
        bootService ! InitFailure
      } else {
        data(classOf[CrawlData]).asInstanceOf[Seq[CrawlData]].foreach(last = _)
        bootService ! InitSuccess
        context.unbecome
        crawl
      }
    case x =>
      log.error("Loading - unknown message" + x)
  }

  override def receive = {
    case Crawl => crawl
    case Upserted(crawlDataId, errors) =>
      log.info("Upserted {}", crawlDataId)
      crawlDataId.collect {
        case (c: CrawlData, id@Some(_)) => last = last.copy(id = id)
      }
    case x =>
      log.error("Unknown message" + x)
  }

  def crawl = {
    val now = Instant.now
    index
      .flatMap(_ => externalLogin)
      .flatMap(_ => internalLogin)
      .flatMap(_ => list(now))
      .flatMap(split _)
      //      .flatMap(details _)
      //      .flatMap(parseDetails _)
      .mapAll(forward(_, now))

  }

  def index: Future[Document] =
    doc(Http().singleRequest(HttpRequest(uri = mainUrl)))

  def doc(response: Future[HttpResponse]): Future[Document] =
    response
      .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
        .map(_.compact.utf8String))
      .map(Jsoup.parse _)

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

  def list(now: Instant = Instant.now): Future[Document] = {
    val tenderDate = listDateFormat.format(Instant.ofEpochMilli(math.max(last.last, now.minusMillis(listBeforeNow).toEpochMilli)).atOffset(ZoneOffset.UTC))
    val url = listUrl.format(tenderDate)
    log.debug("Getting tenders since {}, url {}", tenderDate, url)
    doc(Http().singleRequest(HttpRequest(uri = url)))
  }

  def split(document: Document): Future[List[Tender]] = {
    //TODO error handling and reporting
    Future(
      document.select("article.widget-content.widget-eps-search-result-widget.widget > div.notice-search-item").asScala.toList.map { element =>
        val nameInternalUriElem = element.select("div.notice-search-item-header > a").last
        val internalUrl = Path(nameInternalUriElem.attr("href")).toString
        val name = nameInternalUriElem.text()
        val flag = element.select("div.notice-search-item-header > img").first.attr("src").split("/").last.split("_").last.split("\\.").head
        val publishedByElem: Element = element.select("div.left-col > div:nth-child(1)").first
        val publishedBy = publishedByElem.text.stripPrefix("Publisert av: ")
        val publishedByUrl = Option(publishedByElem.children.first).map(elem => Uri(elem.attr("href").trim).toString)
        val doffinReference = element.select("div.right-col > div:nth-child(1)").first.text.stripPrefix("Doffin referanse: ")
        val announcementType = element.select("div.left-col > div:nth-child(2)").text().stripPrefix("Kunngj\u00f8ringstype: ")
        val announcementDate = parseInstant(element.select("div.right-col > div:gt(0):last-child").text().stripPrefix("Kunngj\u00f8ringsdato: ")).toEpochMilli
        val tenderDeadline = Option(element.select("div.right-col > div:nth-child(2) > span").first).map(elem => parseInstant(elem.text).toEpochMilli)
        val county = Option(element.select("div:gt(3):lt(6):containsOwn(Kommune)").first).map(_.text.stripPrefix("Kommune: "))
        val municipality = Option(element.select("div:gt(3):lt(6):containsOwn(Fylke)").first).map(_.text.stripPrefix("Fylke: "))
        val externalUrl = Option(element.select("div.notice-search-item-header > a.pull-right.doc-icon").first).map(elem => Uri(elem.attr("href").trim).toString)
        Tender(name, internalUrl, flag, publishedBy, publishedByUrl, doffinReference, announcementType, announcementDate, tenderDeadline, county, municipality, externalUrl)
      }
    )
  }

  def parseInstant(date: String) = LocalDate.parse(date, listDateFormat).atStartOfDay.toInstant(ZoneOffset.UTC)

  def details(tenders: List[Tender]): Future[List[(Tender, Document)]] =
    Future.sequence(tenders.map(tender =>
      doc(Http().singleRequest(HttpRequest(uri = s"$mainUrl${tender.internalUrl}")))
        .map(tender -> _)))

  def parseDetails(tenderDetailDocs: List[(Tender, Document)]) = {
    Future.successful(tenderDetailDocs.map { case (tender, detailDoc) =>
      parseDetail(detailDoc)
      tender
    })
  }

  def parseDetail(detailDoc: Document) = {
    val x = detailDoc.select("div.eps-section:nth-child(3)")
    println(x)
  }

  def forward(triedTenders: Try[List[Tender]], now: Instant = Instant.now) = {
    log.debug("Parse done: {}", triedTenders.map(_.size + " tenders parsed"))
    val tenders = triedTenders.toOption.toList.flatten
    if (tenders.nonEmpty)
      tenderService ! NewTenders(tenders)
    last = last.copy(last = now.toEpochMilli)
    dbService ! Upsert(last)
    context.system.scheduler.scheduleOnce(crawlInterval, self, Crawl)
  }
}

object CrawlService {
  val passwordKey = "password"
  val usernameKey = "username"

  case object Crawl
}