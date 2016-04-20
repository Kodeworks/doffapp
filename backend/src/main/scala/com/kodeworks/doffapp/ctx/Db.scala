package com.kodeworks.doffapp.ctx

import akka.actor.{ActorSystem, Props}
import com.kodeworks.doffapp.actor.{CrawlService, TenderService}
import com.kodeworks.doffapp.model._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slickext.macros.table

trait Db {
  val dbConfig: DatabaseConfig[JdbcProfile]

  import dbConfig.driver.api._

  @table[Tender]
  class Tenders

  @table[User]
  class Users

  @table[Classify]
  class Classifys

  @table[CrawlData]
  class CrawlDatas

  val tableQuerys: List[TableQuery[_ <: Table[_]]]
  val tables: Map[Class[_], TableQuery[_ <: Table[_]]]
}

trait DbImpl extends Db {
  this: Prop =>
  println("Loading Db")
  val dbConfig = DatabaseConfig.forConfig[JdbcProfile](dbType)

  import dbConfig.driver.api._

  val tableQuerys = List(
    Tenders, Users, Classifys, CrawlDatas
  )

  val tables: Map[Class[_], TableQuery[_ <: Table[_]]] = Map(
    classOf[Tender] -> Tenders,
    classOf[User] -> Users,
    classOf[Classify] -> Classifys,
    classOf[CrawlData] -> CrawlDatas)

}