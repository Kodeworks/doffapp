package com.kodeworks.doffapp.actor

import java.net.ConnectException

import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.pattern.pipe
import com.kodeworks.doffapp.actor.DbService._
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message
import com.kodeworks.doffapp.message._
import com.kodeworks.doffapp.util.RichFuture
import org.h2.tools.Server

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz.Scalaz._
import scalaz._
import concurrent.duration._

class DbService(val ctx: Ctx) extends Actor with ActorLogging {

  import context.dispatcher
  import ctx._
  import dbConfig.db
  import dbConfig.driver.api._

  var h2WebServer: Server = null

  case class Chill(id: Option[Long], chilling: String, will: Int)

  class Chills(tag: Tag) extends Table[Chill](tag, "chill") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

    def chilling = column[String]("chillings")

    def will = column[Int]("will")

    def * = (id, chilling, will) <>((Chill.apply _).tupled, Chill.unapply)
  }

  override def preStart {
    context.become(initing)
    val inits = ListBuffer[Future[Any]]()
    if (dev) {
      inits += db.run(tableQuerys.map(_.schema).reduce(_ ++ _).create).map { res =>
        log.info("Schema created")
        res
      }
      h2WebServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082")
      h2WebServer.start
    }
    Future.sequence(inits).mapAll {
      case Failure(x)
        if null != x.getCause
          && x.getCause.getClass.isAssignableFrom(classOf[ConnectException]) =>
        log.error("Critical database error. Error connecting to database during boot.")
        ctx.bootService ! InitFailure
      case Failure(x) =>
        log.warning("Non-critical db error: {}", x)
        ctx.bootService ! InitSuccess
        context.unbecome
    }
  }

  override def postStop() {
    db.close
    if (null != h2WebServer)
      h2WebServer.stop()
  }

  def initing = Actor.emptyBehavior

  def down: Receive = {
    val commands = ListBuffer[DbCommand]()

    {
      case c: DbCommand => commands += c
      case x =>
        log.error("Down - unknown message: {} with sender {}", x, sender)
    }
  }

  override def receive = {
    case Load(persistables@_*) =>
      val zelf = self
      val zender = sender
      log.info("Load {}", persistables.map(_.getSimpleName).mkString(", "))
      Future.sequence(persistables
        .flatMap(per => tables.get(per)
          .map(table =>
            db.run(table.result)
              .mapAll {
                case Success(res) => Right(per -> res)
                case Failure(x) => Left(per -> x)
              }
          )).asInstanceOf[Seq[Future[Either[(Class[_], Throwable), (Class[_], Seq[AnyRef])]]]])
        .map(_.toList.separate)
        .map { res =>
          if (res._1.nonEmpty) {
            //            zelf ! Load(res._1.map(_._1): _*)
            goDown(zelf)
          }
          res
        }
        .map(res => Loaded(res._2.toMap, res._1.toMap))
        .pipeTo(zender)

    case Insert(persistables@_ *) =>
      val zelf = self
      val zender = sender
      log.info("Insert")
      Future.sequence(persistables
        .flatMap(per => table(per)
          .map(table =>
            db.run(table += per)
              .mapAll {
                case Success(res) => Right(per)
                case Failure(x) => Left(per -> x)
              }
          )).asInstanceOf[Seq[Future[Either[(AnyRef, Throwable), AnyRef]]]])
        .map(_.toList.separate)
        .map { res =>
          if (res._1.nonEmpty) {
            zelf ! Insert(res._1.map(_._1): _*)
            goDown(zelf)
          }
          res
        }
        .map(res => Inserted(res._2.toList, res._1.toMap))
        .pipeTo(zender)

    case Upsert(persistable@_ *) =>
      val zelf = self
      val zender = sender
      log.info("Upsert")
      Future.sequence(persistable
        .flatMap(per => table(per)
          .map(table =>
            db.run(table.returning(table.map(_.id)).insertOrUpdate(per))
              .map(_.flatten)
              .mapAll {
                case Success(res) => Right(per -> res)
                case Failure(x) => Left(per -> x)
              }
          )).asInstanceOf[Seq[Future[Either[(AnyRef, Throwable), (AnyRef, Option[Long])]]]])
        .map(_.toList.separate)
        .map { res =>
          if (res._1.nonEmpty) {
            zelf ! Upsert(res._1.map(_._1): _*)
            goDown(zelf)
          }
          res
        }
        .map(res => Upserted(res._2.toMap, res._1.toMap))
        .pipeTo(zender)
  }

  def goDown(self: ActorRef = self) {
    //TODO make sure no double entries
    log.info("Going down, upcheck every 5 seconds")
    context.system.scheduler.scheduleOnce(5 seconds, self, UpCheck)
    context.become(down)
  }

  private def table(per: AnyRef) =
    tables.get(per.getClass).map(tableCast(_, per))

  private def tableCast(table: TableQuery[_], per: AnyRef) =
    table.asInstanceOf[TableQuery[Table[AnyRef] {
      val id: Rep[Option[Long]]
    }]]
}

object DbService {

  sealed trait DbMessage

  sealed trait DbInMessage extends DbMessage

  sealed trait DbOutMessage extends DbMessage

  sealed trait DbQuery extends DbInMessage

  sealed trait DbCommand extends DbInMessage

  sealed trait DbControl extends DbMessage

  case class Insert(persistables: AnyRef*) extends DbCommand

  case class Inserted(
                       persistables: List[AnyRef],
                       errors: Map[AnyRef, Throwable] = Map.empty
                     ) extends DbOutMessage

  case class Upsert(persistables: AnyRef*) extends DbCommand

  case class Upserted(
                       persistableIds: Map[AnyRef, Option[Long]],
                       errors: Map[AnyRef, Throwable] = Map.empty
                     ) extends DbOutMessage

  case class Load(persistables: Class[_]*) extends DbQuery

  case class Loaded(
                     data: Map[Class[_], Seq[AnyRef]],
                     errors: Map[Class[_], Throwable] = Map.empty
                   ) extends DbOutMessage

  case object UpCheck extends DbControl

}