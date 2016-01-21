package com.kodeworks.doffapp.actor

import java.net.ConnectException

import akka.actor.{ActorCell, Actor, ActorLogging}
import akka.pattern.pipe
import com.kodeworks.doffapp.actor.DbService._
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message
import org.h2.tools.Server

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}
import message._
import com.kodeworks.doffapp.util.RichFuture

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

  def initing = Actor.emptyBehavior

  override def postStop() {
    db.close
    if (null != h2WebServer)
      h2WebServer.stop()
  }

  override def receive = extractMessage(msg => {
    case Load(persistables@_*) =>
      log.info("Load {}", persistables.map(_.getSimpleName).mkString(", "))
      Future.sequence(persistables
        .flatMap(per => tables.get(per)
          .map(table => db.run(table.result)
            .map(per -> _.asInstanceOf[Seq[AnyRef]]))))
        .map(res => Loaded(res.toMap))
        .pipeTo(sender)
    case Insert(persistable@_*) =>
      log.info("Insert")
      persistable.foreach {
        per =>
          table(per).foreach {
            table =>
              db.run(table += per)
          }
      }
    case Upsert(persistable@_*) =>
      log.info("Upsert")
      Future.sequence(persistable
        .flatMap(per => table(per)
          .map(table =>
            db.run(table.returning(table.map(_.id)).insertOrUpdate(per))
              .map(per -> _.flatten))))
        .map(res => Upserted(res.toMap))
        .pipeTo(sender)
  })

  private def table(per: AnyRef) =
    tables.get(per.getClass).map(tableCast(_, per))

  private def tableCast(table: TableQuery[_], per: AnyRef) =
    table.asInstanceOf[TableQuery[Table[AnyRef] {
      val id: Rep[Option[Long]]
    }]]
}

object DbService {

  sealed trait DbMessage

  case class Insert(persistables: AnyRef*) extends DbMessage

  case class Upsert(persistables: AnyRef*) extends DbMessage

  case class Upserted(persistableIds: Map[AnyRef, Option[Long]]) extends DbMessage

  case class Load(persistables: Class[_]*) extends DbMessage

  case class Loaded(data: Map[Class[_], Seq[AnyRef]]) extends DbMessage

}