package com.kodeworks.doffapp.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.marshallers.argonaut.ArgonautSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.RouteConcatenation.enhanceRouteWithConcatenation
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import argonaut.Argonaut._
import argonaut.Shapeless._
import argonaut._
import com.kodeworks.doffapp.actor.DbService.{Insert, Inserted, Load, Loaded}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.message.{InitFailure, InitSuccess, SaveUsers}
import com.kodeworks.doffapp.model.User
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._

import scala.collection.mutable

class UserService(ctx: Ctx) extends Actor with ActorLogging {

  import ctx._

  implicit val ac = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  implicit def sm = sessionManager

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

  val route =
    pathPrefix("user") {
      post {
        path("create") {
          touchOptionalSession(oneOff, usingCookies) {
            case Some(username) =>
              //TODO check username. Should not be a problem unless we've wiped users or something
              complete(400 -> s"You are already logged in as $username. Please log out before trying to create a new user")
            case _ =>
              val user = User()
              self ! SaveUsers(Seq(user))
              setSession(oneOff, usingCookies, user.name) {
                complete(user)
              }
          }
        } ~
          pathPrefix("login") {
            def login(user: User) =
              validate(users.contains(user.name), s"Unknown user ${user.name}") {
                touchOptionalSession(oneOff, usingCookies) {
                  case Some(username) =>
                    complete(400 -> s"You are already logged in as $username. Please log out before trying to log in with another user")
                  case _ =>
                    log.info("User logged in: {}", user.name)
                    setSession(oneOff, usingCookies, user.name) {
                      complete(s"Welcome back, ${user.name}")
                    }
                }
              }
            path(Segment)(userName => login(User(userName))) ~
              entity(as[User])(login _)
          } ~ path("logout") {
          requiredSession(oneOff, usingCookies) { username =>
            invalidateSession(oneOff, usingCookies) {
              complete(s"$username logged out")
            }
          }
        }
      }
    }

  override def receive = {
    case rc: RequestContext =>
      log.info("userservice rc, thread {}", Thread.currentThread().getId + " " + Thread.currentThread().getName)
      route(rc).pipeTo(sender)
    case SaveUsers(ts) =>
      val newUsers = ts.filter(t => !users.contains(t.name))
      log.info("Got {} users, of which {} were new", ts.size, newUsers.size)
      users ++= newUsers.map(t => t.name -> t)
      if (newUsers.nonEmpty) dbService ! Insert(newUsers: _*)
    case Inserted(data, errors) =>
      log.info("Inserted users: {}", data)
      users ++= data.asInstanceOf[Map[User, Option[Long]]].map {
        case (u, id) => u.name -> u.copy(id = id)
      }
    case x =>
      log.error("Unknown " + x)
  }
}
