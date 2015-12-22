package com.kodeworks.doffapp.service

import java.io.BufferedReader
import javax.script.{Invocable, ScriptEngine, ScriptEngineManager}

import argonaut.{DecodeJson, CodecJson, EncodeJson}
import com.kodeworks.doffapp.model.InputOutput
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.io.Source

trait Brain {
  val engine = new ScriptEngineManager().getEngineByName("nashorn")

  private def loadScript(script: String, engine: ScriptEngine) = {
    val reader: BufferedReader = Source.fromURL(getClass.getClassLoader.getResource(script)).bufferedReader
    try engine.eval(reader)
    finally reader.close
  }

  loadScript("brain.js", engine)
  private val net: ScriptObjectMirror = engine.eval("new brain.NeuralNetwork();").asInstanceOf[ScriptObjectMirror]

  def train[I: Numeric : EncodeJson : DecodeJson, O: Numeric : EncodeJson : DecodeJson](inputOutputs: List[(List[I], List[O])]) = {
    import argonaut._
    import Argonaut._
    implicit val ioc = InputOutput.inputOutputCodec[List[I], List[O]]
    val js = inputOutputs.map { case (in, out) => InputOutput(in, out) }.asJson.nospaces
    import scala.collection.JavaConverters._
    engine.asInstanceOf[Invocable].invokeMethod(net, "train", js, Map(
      "iterations" -> 9000,
      "log" -> true,
      "logPeriod" -> 10,
      "errorThresh" -> 0.00001,
      "learningRate" -> 0.2
    ).asJava)
  }

  def run[I: Numeric : EncodeJson : DecodeJson](input: List[I]) = {
    import argonaut._
    import Argonaut._
    import scala.collection.JavaConverters._
    val js = input.asJson.nospaces
    engine.asInstanceOf[Invocable].invokeMethod(net, "run", js).asInstanceOf[ScriptObjectMirror].to(classOf[java.util.List[Double]]).asScala.toList
  }
}

object Brain extends Brain
