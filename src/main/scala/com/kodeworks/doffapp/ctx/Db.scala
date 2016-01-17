package com.kodeworks.doffapp.ctx

import akka.actor.{ActorSystem, Props}
import com.kodeworks.doffapp.actors.{CrawlService, TenderService}
import com.kodeworks.doffapp.model.{CrawlData, User, Tender}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slickext.macros.table

trait Db {
  this: Ctx =>
  val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("db.h2.mem")

  import dbConfig.driver.api._

  @table[CrawlData]
  class CrawlDatas

  @table[Tender]
  class Tenders

  @table[User]
  class Users

  val tableQuerys = List(
    CrawlDatas, Tenders, Users
  )

  val tables: Map[Class[_], TableQuery[_ <: Table[_]]] = Map(
    classOf[CrawlData] -> CrawlDatas,
    classOf[Tender] -> Tenders,
    classOf[User] -> Users)
}
