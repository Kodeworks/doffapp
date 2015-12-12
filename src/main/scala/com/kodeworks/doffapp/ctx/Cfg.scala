package com.kodeworks.doffapp.ctx

import com.typesafe.config.ConfigFactory

import scala.util.Try

trait Cfg {
  val baseConfig = ConfigFactory.load
  val userConfig =
    ConfigFactory.parseResources(Try(baseConfig.getString("user.name")).getOrElse("") + ".conf")
  val config = userConfig.withFallback(baseConfig)
}
