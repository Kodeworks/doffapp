package com.kodeworks.doffapp.actors

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
import com.kodeworks.doffapp.actors.CrawlService._
import com.kodeworks.doffapp.actors.DbService.{Load, Loaded, Upsert, Upserted}
import com.kodeworks.doffapp.actors.TenderService.SaveTenders
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.model.{CrawlData, Tender}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import com.kodeworks.doffapp.util.RichFuture

class CrawlService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  var last = CrawlData(Instant.now.minusMillis(listBeforeNow).atOffset(ZoneOffset.UTC).toInstant.toEpochMilli)

  override def preStart {
    log.info("born")
    context.become(loading)
    dbService ! Load(classOf[CrawlData])
  }

  val loading: Receive = {
    case Loaded(data) =>
      data(classOf[CrawlData]).asInstanceOf[Seq[CrawlData]].foreach(last = _)
      context.unbecome
      crawl
  }

  override def receive = {
    case Crawl => crawl
    case Upserted(crawlDataId) => crawlDataId.collect {
      case (c: CrawlData, id) => last = c.copy(id = id)
    }
    case x =>
      log.error("Loading - unknown message" + x)
  }

  def crawl = {
    val now = Instant.now
    index
      .flatMap(_ => externalLogin)
      .flatMap(_ => internalLogin)
      .flatMap(_ => list(now))
      .flatMap(split _)
      .map(forward(_, now))
      .mapAll(_ =>
        context.system.scheduler.scheduleOnce(crawlInterval, self, Crawl))
  }

  /*
  .map { tenders =>
    tenders.foreach(tenderService ! _)
    tenders
  }
  .onComplete { case tenders0 =>
    //TODO reactive streams
    tenders0.foreach { tenders1 =>
      last = last.copy(last = System.currentTimeMillis)
      dbService ! Upsert(last)
      val dict: Map[String, Int] = SpellingCorrector.dict(tenders1.map(_.name.toLowerCase).mkString(" "))
      val sc: SpellingCorrector = new SpellingCorrector(dict ++ wordbankDict)
      val cs = new CompoundSplitter(ctx)
      val tenders = tenders1.map(t => t.copy(name = {
        SpellingCorrector.words(t.name.toLowerCase).flatMap { s =>
          //TODO add to common corrections
          val correct: String = sc.correct(s)

          //TODO add full word to word list. Guess base form and other full forms based on second word in split
          cs.split(correct).map { splitted =>
            wordbankWordsFullToBase.getOrElse(splitted, splitted)
          }
        }.mkString(" ")
      }))

      //TODO custom trainClassifier that considers synonyms and gives them an appropriate weight,
      // so that "person" also gives some weight to "human", and that removes variations on words (stemming).
      val examples: List[Example[String, String]] = tenders.map {
        case t if t.name contains "konsulent" => Example("interresting", t.name, t.doffinReference)
        case t => Example("uninterresting", t.name, t.doffinReference)
      }
      val config = LiblinearConfig(cost = 5d, eps = .1d)
      val batchFeaturizer = new TfidfBatchFeaturizer[String](0, Ctx.mostUsedWords.toSet)
      val tfidfFeaturized: Seq[Example[String, Seq[FeatureObservation[String]]]] = batchFeaturizer(examples)
      val leastSignificantWords: List[(String, Double)] = tfidfFeaturized.flatMap(_.features).groupBy(_.feature).mapValues(_.minBy(_.magnitude).magnitude).toList.sortBy(lm => -lm._2)
      val stopwords = leastSignificantWords.take(30).map(_._1).toSet
      val featurizer = new BowFeaturizer(stopwords)

      val classifier = NakContext.trainClassifier(config, featurizer, examples)
      val test = "konsulent tjenester innenfor it og sikkerhet"
      val prediction = classifier.predict(test)
      log.info(s"prediction of '$test': $prediction")
    }

    context.system.scheduler.scheduleOnce(crawlInterval, self, Crawl)
  }
  */

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
    doc(Http().singleRequest(HttpRequest(
      uri = url)))
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

  def forward(tenders: List[Tender], now: Instant = Instant.now) = {
    tenderService ! SaveTenders(tenders)
    last = last.copy(last = now.toEpochMilli)
    dbService ! Upsert(last)
    tenders
  }
}

object CrawlService {
  val passwordKey = "password"
  val usernameKey = "username"

  case object Crawl

}