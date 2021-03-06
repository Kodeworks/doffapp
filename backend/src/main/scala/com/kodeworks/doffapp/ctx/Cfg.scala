package com.kodeworks.doffapp.ctx

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

trait Cfg {
  val baseConfig: Config
  val env: String
  val dev: Boolean
  val test: Boolean
  val prod: Boolean
  val envConfig: Config
  val envConfigFull: Config
  val userConfig: Config
  val userConfigFull: Config
  val systemEnvironment:Config
  val systemProperties:Config
  val config: Config
}

trait CfgImpl extends Cfg {
  println("Loading Cfg")
  override val baseConfig = ConfigFactory.load
  override val env = Try(baseConfig.getString("env").toLowerCase).getOrElse("dev")
  override val dev = "dev" == env
  override val test = "test" == env
  override val prod = "prod" == env
  if (!(dev || test || prod)) {
    println(s"Illegal env $env. Shutting down")
    System.exit(-1)
  }
  println(s"Running in $env mode")
  override val envConfig = ConfigFactory.parseResources(env + ".conf")
  override val envConfigFull = envConfig.withFallback(baseConfig)
  override val userConfig =
    ConfigFactory.parseResources(Try(envConfigFull.getString("user.name")).getOrElse("") + ".conf")
  override val userConfigFull = userConfig.withFallback(envConfigFull)
  override val systemEnvironment = ConfigFactory.systemEnvironment()
  override val systemProperties = ConfigFactory.systemProperties()
  override val config = systemProperties.withFallback(systemEnvironment.withFallback(userConfigFull))
}
