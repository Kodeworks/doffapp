package com.kodeworks.doffapp.ctx

import com.softwaremill.session.{SessionManager, SessionConfig}

trait Http {
  val sessionConfig: SessionConfig
  val sessionManager: SessionManager[String]
}

trait HttpImpl extends Http {
  this: Cfg =>
  println("Loading Http")
  val sessionConfig = SessionConfig.fromConfig(config)
  val sessionManager = new SessionManager[String](sessionConfig)
}
