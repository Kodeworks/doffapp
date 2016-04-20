package com.kodeworks.doffapp.model

case class InputOutput[I, O](input: I, output: O)

object InputOutput {

  import argonaut._
  import Argonaut._

  def inputOutputCodec[I: EncodeJson : DecodeJson, O: EncodeJson : DecodeJson] = {
    casecodec2(InputOutput.apply[I, O], InputOutput.unapply[I, O])("input", "output")
  }
}
