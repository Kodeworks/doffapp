package com.kodeworks.doffapp.ctx

import com.typesafe.config.ConfigFactory

import scala.util.Try

trait Cfg {
  val baseConfig = ConfigFactory.load
  val envConfig = ConfigFactory.parseResources(Try(baseConfig.getString("env")).getOrElse("") + ".conf")
  val envConfigFull = envConfig.withFallback(baseConfig)
  val userConfig =
    ConfigFactory.parseResources(Try(envConfigFull.getString("user.name")).getOrElse("") + ".conf")
  val userConfigFull = userConfig.withFallback(envConfigFull)
  val config = userConfigFull
}
