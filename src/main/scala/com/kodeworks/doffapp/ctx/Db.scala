package com.kodeworks.doffapp.ctx

import akka.actor.{ActorSystem, Props}
import com.kodeworks.doffapp.actors.{CrawlService, TenderService}
import com.kodeworks.doffapp.model.{User, Tender}
import slick.backend.DatabaseConfig
import slick.driver.{H2Driver, JdbcProfile}
import slickext.macros.table

trait Db {
  this: Ctx =>
  val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("db.h2.mem")

  import dbConfig.driver.api._

  @table[Tender]
  class Tenders

  @table[User]
  class Users

  val tableQuerys = List(
    Tenders, Users
  )

  val tables = Map(
    Tender -> Tenders,
    User -> Users)
}
