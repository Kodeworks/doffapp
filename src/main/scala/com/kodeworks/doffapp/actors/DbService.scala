package com.kodeworks.doffapp.actors

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import com.kodeworks.doffapp.actors.DbService._
import com.kodeworks.doffapp.ctx.Ctx
import org.h2.tools.Server

import scala.concurrent.Future
import scala.reflect.runtime.universe.TypeTag
import scala.util.{Success, Failure}

class DbService(val ctx: Ctx) extends Actor with ActorLogging {

  import context.dispatcher
  import ctx._
  import dbConfig.db
  import dbConfig.driver.api._

  //macro does not give code completion, thus this dummy table for development
  class Tenders2(tag: Tag) extends Table[(String, Double)](tag, "tender2") {
    def name = column[String]("COF_NAME")

    def price = column[Double]("PRICE")

    def * = (name, price)
  }

  case class Chill(chilling: String, will: Int)

  class Chills(tag: Tag) extends Table[(String, Int)](tag, "chill") {
    def chilling = column[String]("chillings")

    def will = column[Int]("will")

    def * = (chilling, will)
  }

  override def preStart {
    db.run(tableQuerys.map(_.schema).reduce(_ ++ _).create).onComplete {
      case Success(_) => log.info("Schema created")
      case Failure(_) => log.error("Schema failed")
    }
    //TODO for testing
    val h2WebServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082")
    h2WebServer.start
  }

  override def postStop() {
    db.close
  }

  override def receive = {
    case Load(persistables@_*) =>
      println("persistables\n")
      Future.sequence(persistables
        .flatMap(per => tables.get(per)
          .map(table => db.run(table.result)
            .map(per -> _.asInstanceOf[Seq[AnyRef]]))))
        .map(res => Loaded(res.toMap))
        .pipeTo(sender)
    case Insert(persistable@_*) =>
      persistable.foreach { per =>
        table(per).foreach { table =>
          db.run(table += per)
        }
      }
    case Upsert(persistable@_*) =>
      persistable.foreach { per =>
        table(per).foreach { table =>
          db.run(table.insertOrUpdate(per))
        }
      }
  }

  private def table(per: AnyRef) =
    tables.get(per.getClass).map(cast(_, per))

  private def cast(table: TableQuery[_], per: AnyRef) =
    table.asInstanceOf[TableQuery[Table[per.type]]]
}

object DbService {

  case class Insert(persistables: AnyRef*)

  case class Upsert(persistables: AnyRef*)

  case class Load(persistables: Class[_]*)

  case class Loaded(data: Map[Class[_], Seq[AnyRef]])

}