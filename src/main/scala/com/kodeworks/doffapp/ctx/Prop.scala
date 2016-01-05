package com.kodeworks.doffapp.ctx

import java.time.format.DateTimeFormatter

import scala.concurrent.duration._

trait Prop {
  this: Cfg =>
  val crawlInterval: FiniteDuration = config.getDuration("crawl.interval", MILLISECONDS) millis
  val loginExternalUrl = config.getString("login.external.url")
  val loginInternalUrl = config.getString("login.internal.url")
  val listDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(config.getString("list.date.format"))
  val listBeforeNow = config.getDuration("list.before.now", MILLISECONDS)
  val listUrl = config.getString("list.url")
  val loginPassword = config.getString("login.password")
  val loginUsername = config.getString("login.username")
  val mainUrl = config.getString("main.url")
  val mostUsedWordsCodecName = config.getString("most.used.words.codec.name")
  val mostUsedWordsSrc = config.getString("most.used.words.src")
  val name = config.getString("name")
  val wordbankCodecName = config.getString("wordbank.codec.name")
  val wordbankSrc = config.getString("wordbank.src")
}
