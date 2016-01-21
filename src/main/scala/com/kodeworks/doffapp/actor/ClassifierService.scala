package com.kodeworks.doffapp.actor

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Actor}
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.model.Tender

class ClassifierService(ctx: Ctx) extends Actor with ActorLogging {
  import ctx._

  override def receive = {
    case x => log.error("Unknown " + x)
  }
}

object ClassifierService {
}
