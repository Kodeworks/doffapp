package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.stream.ActorMaterializer
import com.kodeworks.doffapp.actor.DbService.{Insert, Inserted, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess, SaveUsers}
import com.kodeworks.doffapp.model.User

import scala.collection.mutable

class UserService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  val users = mutable.Map[String, User]()

  override def preStart {
    context.become(initing)
    dbService ! Load(classOf[User])
  }

  val initing: Receive = {
    case Loaded(data, errors) =>
      if (errors.nonEmpty) {
        log.error("Critical database error. Error loading data during boot.")
        bootService ! InitFailure
      } else {
        users ++= data(classOf[User]).asInstanceOf[Seq[User]].map(u => u.name -> u)
        log.info("Loaded {} users", users.size)
        bootService ! InitSuccess
        context.unbecome
      }
    case x =>
      log.error("Loading - unknown message" + x)
  }

  override def receive = {
    case SaveUsers(ts) =>
      val newUsers = ts.filter(t => !users.contains(t.name))
      log.info("Got {} users, of which {} were new", ts.size, newUsers.size)
      users ++= newUsers.map(t => t.name -> t)
      if (newUsers.nonEmpty) dbService ! Insert(newUsers: _*)
    case Inserted(data, errors) =>
      log.info("Inserted users: {}", data)
      users ++= data.asInstanceOf[List[User]].map(t => t.name -> t)
    case x =>
      log.error("Unknown " + x)
  }
}