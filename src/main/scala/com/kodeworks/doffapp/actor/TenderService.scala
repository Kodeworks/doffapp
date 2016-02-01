package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.actor.DbService.{Insert, Inserted, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess, SaveTenders}
import com.kodeworks.doffapp.model.{User, Tender}
import com.kodeworks.doffapp.nlp.{CompoundSplitter, SpellingCorrector}
import akka.http.scaladsl.marshallers.argonaut.ArgonautSupport._
import Tender.Json._
import akka.pattern.pipe

import akka.pattern.ask
import scala.collection.mutable

class
TenderService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  implicit val to = timeout

  val tenders = mutable.Map[String, Tender]()
  val processedNames = mutable.Map[String, String]()
  val dict = mutable.Map() ++= wordbankDict
  val spellingCorrector = new SpellingCorrector(dict)
  val compoundSplitter = new CompoundSplitter(ctx)

  override def preStart {
    context.become(initing)
    dbService ! Load(classOf[Tender])
  }

  val initing: Receive = {
    case Loaded(data, errors) =>
      if (errors.nonEmpty) {
        log.error("Critical database error. Error loading data during boot.")
        bootService ! InitFailure
      } else {
        newTenders(data(classOf[Tender]).asInstanceOf[Seq[Tender]])
        log.info("Loaded {} tenders", tenders.size)
        bootService ! InitSuccess
        context.unbecome
      }
    case x =>
      log.error("Loading - unknown message" + x)
  }

  val route = pathPrefix("tender") {
    get {
      complete(tenders.map(_._2))
    }
  }

  override def receive = {
    case rc: RequestContext =>
      log.info("tenderservice rc, thread {}", Thread.currentThread().getId + " " + Thread.currentThread().getName)
      route(rc).pipeTo(sender)
    case SaveTenders(ts) =>
      val newTenders0 = ts.filter(t => !tenders.contains(t.doffinReference))
      log.info("Got {} tenders, of which {} were new", ts.size, newTenders0.size)
      newTenders(newTenders0)
      if (newTenders0.nonEmpty) dbService ! Insert(newTenders0: _*)
    case Inserted(data, errors) =>
      log.info("Inserted tenders: {}", data)
      tenders ++= data.asInstanceOf[Map[Tender, Option[Long]]].map {
        case (t, id) => t.doffinReference -> t.copy(id = id)
      }
    case x =>
      log.error("Unknown " + x)
  }

  def newTenders(ts: Seq[Tender]) {
    val tendersDict: Map[String, Int] = SpellingCorrector.dict(ts.map(_.name.toLowerCase).mkString(" "))
    tendersDict.foreach { case (word, count) =>
      dict(word) = {
        val count1 = count + dict.getOrElse(word, 0)
        if (0 >= count1) Int.MaxValue
        else count1
      }
    }
    processedNames ++= ts.map(tender => tender.doffinReference ->
      SpellingCorrector.words(tender.name.toLowerCase).flatMap { s =>
        //TODO add to common corrections
        val correct: String = spellingCorrector.correct(s)
        //TODO add full word to word list. Guess base form and other full forms based on second word in split
        compoundSplitter.split(correct).map(wordbankWordsFullToBase _)
      }.mkString(" ")
    )
    tenders ++= ts.map(t => t.doffinReference -> t)
  }
}
