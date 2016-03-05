package com.kodeworks.doffapp

import java.time.format.DateTimeFormatter

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.kodeworks.doffapp.ctx.Ctx
import com.kodeworks.doffapp.nlp.BowTfIdfFeaturizer
import com.kodeworks.doffapp.nlp.wordbank.Word
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.typesafe.config.Config
import nak.classify.NaiveBayes.Trainer
import nak.data.TfidfBatchFeaturizer
import nak.liblinear.LiblinearConfig
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.model.Table

import scala.concurrent.duration.FiniteDuration
import scala.io.{Codec, Source}

trait TestCtx extends Ctx {
  override val baseConfig: Config = null
  override val env: String = null
  override val dev: Boolean = false
  override val test: Boolean = false
  override val prod: Boolean = false
  override val envConfig: Config = null
  override val envConfigFull: Config = null
  override val userConfig: Config = null
  override val userConfigFull: Config = null
  override val systemEnvironment:Config = null
  override val systemProperties:Config = null
  override val config: Config = null
  override val bootInitTimeout: Long = 0L
  override val crawlInterval: FiniteDuration = null
  override val dbH2Server: Boolean = false
  override val dbSchemaCreate: Boolean = false
  override val dbType: String = null
  override val httpInterface: String = null
  override val httpPort: Int = 0
  override val loginExternalUrl: String = null
  override val loginInternalUrl: String = null
  override val listDateFormat: DateTimeFormatter = null
  override val listBeforeNow: Long = 0L
  override val listUrl: String = null
  override val logbackConfigurationFile: Option[String] = null
  override val loginPassword: String = null
  override val loginUsername: String = null
  override val mainUrl: String = null
  override val mostUsedWordsCodecName: String = null
  override val mostUsedWordsSrc: String = null
  override val name: String = null
  override val timeout: Timeout = null
  override val wordbankCodecName: String = null
  override val wordbankSrc: String = null
  override val mostUsedWordsCodec: Codec = null
  override val mostUsedWordsSource: Source = null
  override val wordbankCodec: Codec = null
  override val wordbankSource: Source = null
  override val wordbankWords: List[Word] = null
  override val wordbankWordsFull: List[String] = null
  override val wordbankDict: Map[String, Int] = null
  override val wordbankWordsFullToBases: Map[String, List[String]] = null
  override val wordbankWordsBaseToFulls: Map[String, List[String]] = null
  override def wordbankWordsFullToBase(word: String): String = null
  override val mostUsedWords: List[String] = null
  override val mostUsedWordsTop64: Set[String] = null
  override val liblinearConfig: LiblinearConfig = LiblinearConfig(cost = 5d, eps = .1)
  override val tfidfBatchFeaturizer: TfidfBatchFeaturizer[String] = null
  override val bowFeaturizer: BowTfIdfFeaturizer = null
  override val naiveBayes: Trainer[String, String] = null
  override val classifyLabels: Array[String] = null
  override val dbConfig: DatabaseConfig[JdbcProfile] = null
  override val tableQuerys = null
  override val tables = null
  override val actorSystem: ActorSystem = null
  override val bootService: ActorRef = null
  override val sessionConfig: SessionConfig = null
  override val sessionManager: SessionManager[String] = null
}
