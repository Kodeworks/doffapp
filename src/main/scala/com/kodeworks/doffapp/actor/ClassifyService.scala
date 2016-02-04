package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp
import com.kodeworks.doffapp.actor.DbService.{Inserted, Insert, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message._
import com.kodeworks.doffapp.model.{Tender, Classify}
import akka.pattern.{pipe, ask}
import scala.collection.mutable
import akka.http.scaladsl.marshallers.argonaut.ArgonautSupport._
import Classify.Json._
import doffapp.nlp._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import akka.http.scaladsl.model.StatusCodes._

//TODO custom trainClassifier that considers synonyms and gives them an appropriate weight,
// so that "person" also gives some weight to "human", and that removes variations on words (stemming).
class ClassifyService(ctx: Ctx) extends Actor with ActorLogging {
  //TODO import Implicits for these imports/implicits
  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  implicit val to = timeout

  implicit def sm = sessionManager

  val dict = mutable.Map() ++= wordbankDict
  val spellingCorrector = new SpellingCorrector(dict)
  val compoundSplitter = new CompoundSplitter(ctx)

  var classifierFactory: ClassifierFactory = null
  val processedNames = mutable.Map[String, String]()
  val classifys = mutable.Set[Classify]()
  val userClassifiers = mutable.Map[String, Classifier]()

  override def preStart {
    context.become(initing)
    tenderService ! ListenTenders
  }

  val initing: Receive = {
    case ListenTendersReply(tenders) =>
      log.info("Got {} tenders", tenders.size)
      newTenders(tenders)
      dbService ! Load(classOf[Classify])
    case Loaded(data, errors) =>
      if (errors.nonEmpty) {
        log.error("Critical databases error. Error loading data during boot.")
        bootService ! InitFailure
      } else {
        newClassifys(data(classOf[Classify]).asInstanceOf[Seq[Classify]])
        log.info("Loaded {} classifys", classifys.size)
        bootService ! InitSuccess
        context.unbecome
      }
    case x =>
      log.error("Loading - unknown message" + x)
  }

  val route =
    (pathPrefix("classify") & requiredSession(oneOff, usingCookies)) { user =>
      (get & pathPrefix("tender")) {
        path("processed") {
          log.info("classify/tender/processed")
          complete(processedNames.map(kv => Map("doffinReference" -> kv._1, "name" -> kv._2)))
        } ~
          path(Segment) { tender =>
            log.info("classify/tender/[tender]")
            userClassifiers.get(user) match {
              case Some(classifier) =>
                (rc: RequestContext) =>
                  processedNames.get(tender) match {
                    case Some(pn) =>
                      rc.complete(
                        classifier.tfidf(pn).map(kv => "tfidf_" + kv._1 -> kv._2) ++
                          classifier.bow(pn).map(kv => "bow_" + kv._1 -> kv._2))
                    case _ => rc.complete("No such tender")
                  }
              case _ =>
                complete(400 -> "You have not yet classified any tenders. Classify ~5 tenders and try again")
            }
          } ~
          complete(NotFound)
      } ~
        get {
          log.info("classify")
          complete(classifys.filter(_.user == user))
        } ~
        (post & path(Segment / IntNumber)) { (tenderDoffinReference, label) =>
          validate(0 == label || 1 == label, "Label must be 0 (uninterresting) or 1 (interresting)") {
            log.info("New classify: {} -> {}", tenderDoffinReference, label)
            val classify = Classify(user, tenderDoffinReference, label.toString)
            self ! SaveClassifys(Seq(classify))
            complete(classify)
          }
        }
    }

  override def receive = {
    case rc: RequestContext =>
      route(rc).pipeTo(sender)
    case SaveClassifys(cs) =>
      val newClassifys0 = cs.filter(c => !classifys.contains(c))
      val newClassifys1 = newClassifys0.filter(c => processedNames.contains(c.tender))
      log.info("Got {} classifys, of which {} were new, of which {} had valid tender id", cs.size, newClassifys0.size, newClassifys1.size)
      if (newClassifys1.nonEmpty) {
        newClassifys(newClassifys1)
        dbService ! Insert(newClassifys1: _*)
      }
    case Inserted(data, errors) =>
      log.info("Inserted classifys: {}", data)
      classifys ++= data.asInstanceOf[Map[Classify, Option[Long]]].map {
        case (c, id) => c.copy(id = id)
      }
    case ListenTendersReply(tenders) =>
      newTenders(tenders)
    case x => log.error("Unknown " + x)
  }

  def newTenders(ts: Seq[Tender]) {
    if (ts.nonEmpty) {
      log.info("Got {} new tenders", ts.size)
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
      classifierFactory = new ClassifierFactory(ctx, processedNames.map(_._2).toSeq)
      regenerateClassifiers(userClassifiers.map(_._1).toSeq)
    }
  }

  //TODO think about reclassifications
  def newClassifys(cs: Seq[Classify]) {
    classifys ++= cs
    regenerateClassifiers(cs.map(_.user).distinct)
  }

  def regenerateClassifiers(users: Seq[String]) {
    val newUserClassifers = users.map(user => user -> classifier(user))
    log.info("Classifier updated for users {}", newUserClassifers.map(_._1).mkString(", "))
    userClassifiers ++= newUserClassifers
    //TODO actually classify all tenders for each user
  }

  def classifier(user: String): Classifier = {
    val userClassifys: Map[String, Classify] = classifys.filter(_.user == user).map(c => c.tender -> c).toMap
    val examples = processedNames.toSeq.collect { case (t, pn) if userClassifys.contains(t) =>
      userClassifys(t).label -> pn
    }
    // classifierFactory can be null, but only when we haven't seen any tenders.
    // When we get here, we are guaranteed to have seen at least one tender.
    classifierFactory.classifier(examples)
  }
}

object ClassifyService {
}
