package com.kodeworks.doffapp.ctx

trait Prop {
  this: Cfg =>
  val name = config.getString("name")
  val targetUrl = config.getString("target.url")
}
