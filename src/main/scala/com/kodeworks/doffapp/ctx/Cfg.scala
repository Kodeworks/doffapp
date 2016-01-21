package com.kodeworks.doffapp.ctx

import com.typesafe.config.ConfigFactory

import scala.util.Try

trait Cfg {
  val baseConfig = ConfigFactory.load
  val env = Try(baseConfig.getString("env").toLowerCase).getOrElse("dev")
  val dev = "dev" == env
  val test = "test" == env
  val prod = "prod" == env
  val envConfig = ConfigFactory.parseResources(env + ".conf")
  val envConfigFull = envConfig.withFallback(baseConfig)
  val userConfig =
    ConfigFactory.parseResources(Try(envConfigFull.getString("user.name")).getOrElse("") + ".conf")
  val userConfigFull = userConfig.withFallback(envConfigFull)
  val config = userConfigFull
}
