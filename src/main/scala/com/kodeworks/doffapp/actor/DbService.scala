package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import com.kodeworks.doffapp.actor.DbService._
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message
import org.h2.tools.Server

import scala.concurrent.Future
import scala.util.{Failure, Success}
import message._
import com.kodeworks.doffapp.util.RichFuture

class DbService(val ctx: Ctx) extends Actor with ActorLogging {

  import context.dispatcher
  import ctx._
  import dbConfig.db
  import dbConfig.driver.api._

  case class Chill(id: Option[Long], chilling: String, will: Int)

  class Chills(tag: Tag) extends Table[Chill](tag, "chill") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

    def chilling = column[String]("chillings")

    def will = column[Int]("will")

    def * = (id, chilling, will) <>((Chill.apply _).tupled, Chill.unapply)
  }

  override def preStart {
    context.become(initing)
    if (dev) {
      db.run(tableQuerys.map(_.schema).reduce(_ ++ _).create).mapAll { res =>
        res match {
          case Success(_) => log.info("Schema created")
          case Failure(_) => log.error("Schema failed")
        }
        ctx.bootService ! Inited
        context.unbecome
      }
      val h2WebServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082")
      h2WebServer.start
    }
  }

  def initing = Actor.emptyBehavior

  override def postStop() {
    db.close
  }

  override def receive = {
    case Load(persistables@_*) =>
      log.info("Load {}", persistables.map(_.getSimpleName).mkString(", "))
      Future.sequence(persistables
        .flatMap(per => tables.get(per)
          .map(table => db.run(table.result)
            .map(per -> _.asInstanceOf[Seq[AnyRef]]))))
        .map(res => Loaded(res.toMap))
        .pipeTo(sender)
    case Insert(persistable@_*) =>
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

  case class Insert(persistables: AnyRef*) extends DbMessage

  case class Upsert(persistables: AnyRef*) extends DbMessage

  case class Upserted(persistableIds: Map[AnyRef, Option[Long]]) extends DbMessage

  case class Load(persistables: Class[_]*) extends DbMessage

  case class Loaded(data: Map[Class[_], Seq[AnyRef]]) extends DbMessage

}