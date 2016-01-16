package com.kodeworks.doffapp.actors

import akka.actor.{Actor, ActorLogging}
import com.kodeworks.doffapp.actors.DbService.{Loaded, Load}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.model.Tender
import org.h2.tools.Server
import scala.concurrent.Future
import scala.reflect.runtime.universe.TypeTag
import scala.util.{Failure, Success}
import akka.pattern.pipe

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
    def f[T: TypeTag](t: T) {
      println(implicitly[TypeTag[T]])
    }
    f(tableQuerys.getClass)
    db.run(tableQuerys.map(_.schema).reduce(_ ++ _).create).onComplete {
      case Success(_) => log.info("Schema created")
      case Failure(_) => log.error("Schema failed")
    }


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
            .map(per -> _))))
        .map(res => Loaded(res.toMap))
        .pipeTo(sender)
    case t: Tender =>
      db.run(Tenders += t)
  }
}

object DbService {

  case class Load(persistables: AnyRef*)

  case class Loaded(data: Map[AnyRef, Seq[AnyRef]])

}