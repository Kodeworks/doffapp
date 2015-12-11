package com.kodeworks.doffapp.ctx

import akka.actor.{ActorSystem, Props}
import com.kodeworks.doffapp.actors.MainCrawler

trait Actors {
  this: Ctx =>
  val actorSystem = ActorSystem(name)
  val mainCrawler = actorSystem.actorOf(Props(new MainCrawler(this)))
}
