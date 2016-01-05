package com.kodeworks.doffapp.ctx

import java.io.File

import scala.io.{Codec, Source}
import scala.util.Try

//TODO limit responsibility of this file to make sure file is at correct location, delegate parsing to elsewhere
trait Files {
  this: Prop =>
  def validSource(src: => Source)(implicit codec: Codec): Option[Source] =
    Try(if (src.hasNext) Some(src) else None).toOption.flatten

  val mostUsedWordsCodec = Codec(mostUsedWordsCodecName)

  val mostUsedWordsSource: Source = {
    implicit val codec = mostUsedWordsCodec
    validSource(Source.fromFile(mostUsedWordsSrc))
      .orElse(validSource(Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(mostUsedWordsSrc))))
      .get
  }
  val wordbankCodec = Codec(wordbankCodecName)

  val wordbankSource: Source = {
    implicit val codec = wordbankCodec
    validSource(Source.fromFile(wordbankSrc))
      .get
  }
}
