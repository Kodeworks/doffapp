package com.kodeworks.doffapp

import java.io.BufferedReader
import javax.script.{ScriptEngine, ScriptEngineManager}

import org.junit.Test

import scala.io.Source

class TestJs {
  def loadScript(script: String, engine: ScriptEngine) = {
    val reader: BufferedReader = Source.fromURL(getClass.getClassLoader.getResource(script)).bufferedReader
    try engine.eval(reader)
    finally reader.close
  }

  @Test
  def testJs {
    val engine = new ScriptEngineManager().getEngineByName("nashorn")
    loadScript("jvm-npm.js", engine)
    loadScript("underscore.js", engine)
    loadScript("lookup.js", engine)
    loadScript("cross-validate.js", engine)
    loadScript("neuralnetwork.js", engine)
    loadScript("brain.js", engine)
    loadScript("main.js", engine)
  }
}
