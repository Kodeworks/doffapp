package com.kodeworks.doffapp.ctx

import java.time.format.DateTimeFormatter
import concurrent.duration._

trait Prop {
  this: Cfg =>
  val crawlInterval: FiniteDuration = config.getDuration("crawl.interval", MILLISECONDS) millis
  val externalLoginUrl: String = config.getString("login.external.url")
  val internalLoginUrl: String = config.getString("login.internal.url")
  val name: String = config.getString("name")
  val listDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(config.getString("list.date.format"))
  val listBeforeNow = config.getDuration("list.before.now", MILLISECONDS)
  val listUrl = config.getString("list.url")
  val loginPassword = config.getString("login.password")
  val loginUsername = config.getString("login.username")
  val mainUrl = config.getString("main.url")
}
