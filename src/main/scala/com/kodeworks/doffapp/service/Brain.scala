package com.kodeworks.doffapp.service

import java.io.BufferedReader
import javax.script.{ScriptEngine, ScriptEngineManager}

import scala.io.Source

trait Brain {
  val engine = new ScriptEngineManager().getEngineByName("nashorn")

  private def loadScript(script: String, engine: ScriptEngine) = {
    val reader: BufferedReader = Source.fromURL(getClass.getClassLoader.getResource(script)).bufferedReader
    try engine.eval(reader)
    finally reader.close
  }

  loadScript("jvm-npm.js", engine)
  loadScript("underscore.js", engine)
  loadScript("lookup.js", engine)
  loadScript("cross-validate.js", engine)
  loadScript("neuralnetwork.js", engine)
  loadScript("brain.js", engine)
  loadScript("main.js", engine)
}

object Brain extends Brain
