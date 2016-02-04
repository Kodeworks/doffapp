package com.kodeworks.doffapp.ctx

import java.time
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.Try

trait Prop {
  val bootInitTimeout: Long
  val crawlInterval: FiniteDuration
  val dbType: String
  val httpInterface: String
  val httpPort: Int
  val loginExternalUrl: String
  val loginInternalUrl: String
  val listDateFormat: DateTimeFormatter
  val listBeforeNow: Long
  val listUrl: String
  val logbackConfigurationFile: Option[String]
  val loginPassword: String
  val loginUsername: String
  val mainUrl: String
  val mostUsedWordsCodecName: String
  val mostUsedWordsSrc: String
  val name: String
  val timeout: Timeout
  val wordbankCodecName: String
  val wordbankSrc: String
}

trait PropImpl extends Prop {
  this: Cfg =>
  println("Loading Prop")
  override val bootInitTimeout = config.getDuration("boot.init.timeout").toMillis
  override val crawlInterval: FiniteDuration = config.getDuration("crawl.interval", MILLISECONDS) millis
  override val dbType = config.getString("db.type")
  override val httpInterface = config.getString("http.interface")
  override val httpPort = config.getInt("http.port")
  override val loginExternalUrl = config.getString("login.external.url")
  override val loginInternalUrl = config.getString("login.internal.url")
  override val listDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(config.getString("list.date.format"))
  override val listBeforeNow = config.getDuration("list.before.now", MILLISECONDS)
  override val listUrl = config.getString("list.url")
  override val logbackConfigurationFile = Try(config.getString("logback.configuration.file")).map { lbc =>
    System.setProperty("logback.configurationFile", lbc)
    lbc
  }.toOption
  override val loginPassword = config.getString("login.password")
  override val loginUsername = config.getString("login.username")
  override val mainUrl = config.getString("main.url")
  override val mostUsedWordsCodecName = config.getString("most.used.words.codec.name")
  override val mostUsedWordsSrc = config.getString("most.used.words.src")
  override val name = config.getString("name")
  override val timeout = Timeout(config.getDuration("timeout", TimeUnit.MILLISECONDS) millis)
  override val wordbankCodecName = config.getString("wordbank.codec.name")
  override val wordbankSrc = config.getString("wordbank.src")
}
