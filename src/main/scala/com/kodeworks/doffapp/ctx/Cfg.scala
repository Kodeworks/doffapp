package com.kodeworks.doffapp.ctx

import com.typesafe.config.ConfigFactory

trait Cfg {
  val config = ConfigFactory.load
}
