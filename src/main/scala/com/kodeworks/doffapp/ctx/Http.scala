package com.kodeworks.doffapp.ctx

import com.softwaremill.session.{SessionManager, SessionConfig}

trait Http {
  this: Ctx =>
  println("Loading Http")

  val sessionConfig = SessionConfig.fromConfig(config)
  val sessionManager = new SessionManager[String](sessionConfig)
}
