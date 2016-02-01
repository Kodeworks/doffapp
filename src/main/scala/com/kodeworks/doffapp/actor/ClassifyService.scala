package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.actor.ClassifyService.NewUserClassifiers
import com.kodeworks.doffapp.actor.DbService.{Inserted, Insert, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message._
import com.kodeworks.doffapp.model.{Tender, Classify}
import akka.pattern.{pipe, ask}
import nak.NakContext
import nak.core.{FeaturizedClassifier, IndexedClassifier}
import nak.data.{BowFeaturizer, FeatureObservation, Example}
import scala.collection.mutable
import akka.http.scaladsl.marshallers.argonaut.ArgonautSupport._
import Classify.Json._

import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._

import scala.concurrent.Future

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

  val classifys = mutable.Set[Classify]()
  val userClassifiers = mutable.Map[String, IndexedClassifier[String] with FeaturizedClassifier[String, String]]()

  override def preStart {
    context.become(initing)
    dbService ! Load(classOf[Classify])
  }

  val initing: Receive = {
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
      (get & path("tender" / Segment)) { tender =>
        userClassifiers.get(user) match {
          case Some(classifier) =>
            (rc: RequestContext) =>
              tenderService ? GetTenderProcessedNames(Set(tender)) flatMap {
                case GetTenderProcessedNamesReply(pns) if pns.nonEmpty =>
                  rc.complete(classifier.predict(pns(tender)))
                case _ => rc.complete("No such tender")
              }
          case _ =>
            complete(400 -> "You have not yet classified any tenders. Classify ~5 tenders and try again")
        }
      } ~
        get {
          complete(classifys.filter(_.user == user))
        } ~
        (post & path(Segment / IntNumber)) { (tenderDoffinReference, classifyNumber) =>
          validate(0 == classifyNumber || 1 == classifyNumber, "Classify-number must be 0 (uninterresting) or 1 (interresting)") {
            val classify = Classify(user, tenderDoffinReference, if (1 == classifyNumber) true else false)
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
      log.info("Got {} classifys, of which {} were new", cs.size, newClassifys0.size)
      newClassifys(cs)
      if (newClassifys0.nonEmpty) dbService ! Insert(newClassifys0: _*)
    case NewUserClassifiers(ucs) =>
      log.info("Classifier updated for users {}", ucs.keys.mkString(", "))
      userClassifiers ++= ucs
    case Inserted(data, errors) =>
      log.info("Inserted classifys: {}", data)
      classifys ++= data.asInstanceOf[Map[Classify, Option[Long]]].map {
        case (c, id) => c.copy(id = id)
      }
    case x => log.error("Unknown " + x)
  }

  def newClassifys(cs: Seq[Classify]) {
    classifys ++= cs
    val tendersFuture: Future[Map[String, String]] = (tenderService ? GetTenderProcessedNames).mapTo[GetTenderProcessedNamesReply].map(_.processedNames)
    Future.sequence(cs.map(_.user).distinct.map(user => newUserClassifys(user, tendersFuture)))
      .map(ucs => NewUserClassifiers(ucs.toMap))
      .pipeTo(self)
    //TODO actually classify all tenders
  }

  def newUserClassifys(user: String, tendersFuture: Future[Map[String, String]]) = {
    val userClassifys = classifys.filter(_.user == user).map(c => c.tender -> c).toMap
    tendersFuture.map { ts =>
      val tenders = ts.filter { case (t, _) => userClassifys.contains(t) }
      val examples: Seq[Example[String, String]] = tenders.map { case (t, pn) =>
        Example(if (userClassifys(t).interresting) "1" else "0", pn, t)
      }.toSeq
      val tfidfFeaturized: Seq[Example[String, Seq[FeatureObservation[String]]]] = batchFeaturizer(examples)
      val leastSignificantWords: List[(String, Double)] = tfidfFeaturized.flatMap(_.features).groupBy(_.feature).mapValues(_.minBy(_.magnitude).magnitude).toList.sortBy(lm => -lm._2)
      val stopwords: Set[String] = leastSignificantWords.take(30).map(_._1).toSet
      val featurizer: BowFeaturizer = new BowFeaturizer(stopwords)
      user -> NakContext.trainClassifier(liblinerConfig, featurizer, examples)
    }
  }
}

object ClassifyService {

  case class NewUserClassifiers(userClassifiers: Map[String, IndexedClassifier[String] with FeaturizedClassifier[String, String]])

}
