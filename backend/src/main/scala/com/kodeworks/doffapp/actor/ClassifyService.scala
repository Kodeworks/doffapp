package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.marshallers.argonaut.ArgonautSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import argonaut.Argonaut._
import argonaut.Shapeless._
import argonaut._
import com.kodeworks.doffapp.actor.DbService.{Insert, Inserted, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message._
import com.kodeworks.doffapp.model.{Classification, Classify, Tender}
import com.kodeworks.doffapp.nlp._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._

import scala.collection.mutable

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

  //word -> count
  val dict = mutable.Map() ++= wordbankDict
  val spellingCorrector = new SpellingCorrector(dict)
  val compoundSplitter = new CompoundSplitter(ctx)

  var classifierFactory: ClassifierFactory = null
  //tender -> processed name
  val processedNames = mutable.Map[String, String]()
  val classifys = mutable.Set[Classify]()
  //user -> classifier
  val userClassifiers = mutable.Map[String, Classifier]()
  //user -> classification
  val userClassifications = mutable.Map[String, Seq[Classification]]()

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
      (get & path("processedtender")) {
        log.info("Get all tender refs and processed names")
        complete(processedNames.map(kv => Map("doffinReference" -> kv._1, "name" -> kv._2)))
      } ~
        (get & pathPrefix("classification")) {
          path("tfidf") {
            userClassifications.get(user) match {
              case Some(classifications) => complete(classifications.sortBy(-_.tfidf))
              case _ => complete("No classifications for user")
            }
          } ~
            path("bow") {
              userClassifications.get(user) match {
                case Some(classifications) => complete(classifications.sortBy(-_.bow))
                case _ => complete("No classifications for user")
              }
            } ~
            path(Segment) { tender =>
              log.info("Get tfidf and bow classifications for tender {}", tender)
              userClassifications.get(user).flatMap(
                _.find(_.tender == tender)) match {
                case Some(classification) => complete(classification)
                case _ => userClassifiers.get(user) match {
                  case Some(classifier) =>
                    rc =>
                      processedNames.get(tender) match {
                        case Some(pn) =>
                          rc.complete(classification(user, tender))
                        case _ => rc.complete("No such tender")
                      }
                  case _ =>
                    complete(400 -> "You have not yet classified any tenders. Classify ~5 tenders and try again")
                }
              }
            } ~ {
            userClassifications.get(user) match {
              case Some(classifications) => complete(classifications)
              case _ => complete("No classifications for user")
            }
          }
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
    case GetClassifications(user) =>
      log.info("Get classifications for user {}", user)
      sender ! GetClassificationsReply(userClassifications.get(user).map(_.sortBy(-_.weighted)))
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
      //TODO skip 1-2-(3?) length words
      processedNames ++= ts.map(tender => tender.doffinReference ->
        SpellingCorrector.words(tender.name.toLowerCase).flatMap { s =>
          //TODO add to common corrections
          val correct: String = spellingCorrector.correct(s)
          //TODO add full word to word list. Guess base form and other full forms based on second word in split
          compoundSplitter.split(correct).map(wordbankWordsFullToBase _)
        }.mkString(" ")
      )
      classifierFactory = new ClassifierFactory(ctx, processedNames.map(_._2).toSeq)
      regenerateClassifiersAndClassifications(userClassifiers.map(_._1).toSeq)
    }
  }

  def newClassifys(cs: Seq[Classify]) {
    classifys ++= cs
    regenerateClassifiersAndClassifications(cs.map(_.user).distinct)
  }

  def regenerateClassifiersAndClassifications(users: Seq[String]) {
    val newUserClassifers = users.map(user => user -> classifier(user))
    log.info("Classifier updated for users {}", newUserClassifers.map(_._1).mkString(", "))
    userClassifiers ++= newUserClassifers
    val newUserClassifications = users
      .map(user => user -> userRecentClassifys(user).values)
      .map { case (user, cs) =>
        val classifyd = cs.map(_.tender).toSet
        val unclassifyd = processedNames.collect {
          case (tender, _) if !classifyd.contains(tender) => tender
        }.toSeq
        user -> unclassifyd.flatMap(classification(user, _))
      }
    userClassifications ++= newUserClassifications
  }

  def classifier(user: String): Classifier = {
    val ucs: Map[String, Classify] = userRecentClassifys(user)
    val examples = processedNames.toSeq.collect { case (t, pn) if ucs.contains(t) =>
      ucs(t).label -> pn
    }
    // classifierFactory can be null, but only when we haven't seen any tenders.
    // When we get here, we are guaranteed to have seen at least one tender.
    classifierFactory.classifier(examples)
  }

  //most recent classifys
  def recentClassifys: Seq[Classify] =
    classifys
      .groupBy(_.user)
      .flatMap(_._2.groupBy(_.tender)
        .map { case (tender, classifys0) =>
          tender -> classifys0.maxBy(_.time)
        }
      ).values.toSeq

  // returns tender -> most recent classify
  def userRecentClassifys(user: String): Map[String, Classify] =
    classifys
      .filter(_.user == user)
      .groupBy(_.tender)
      .map { case (tender, classifys0) =>
        tender -> classifys0.maxBy(_.time)
      }

  def classification(user: String, tender: String) = {
    userClassifiers.get(user).flatMap { classifier =>
      processedNames.get(tender).map(
        classifier.classification(tender, user, _)
      )
    }
  }
}

object ClassifyService {
}
