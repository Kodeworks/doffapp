package com.kodeworks.doffapp.ctx

import scala.io.{Codec, Source}
import scala.util.Try

//TODO limit responsibility of this file to make sure file is at correct location, delegate parsing to elsewhere
trait Files {
  val mostUsedWordsCodec: Codec
  val mostUsedWordsSource: Source
  val wordbankCodec: Codec
  val wordbankSource: Source
}

trait FilesImpl extends Files {
  this: Prop =>
  println("Loading Files")

  def validSource(src: => Source)(implicit codec: Codec): Option[Source] =
    Try(if (src.hasNext) Some(src) else None).toOption.flatten

  override val mostUsedWordsCodec = Codec(mostUsedWordsCodecName)

  override val mostUsedWordsSource: Source = {
    implicit val codec = mostUsedWordsCodec
    validSource(Source.fromFile(mostUsedWordsSrc))
      .orElse(validSource(Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(mostUsedWordsSrc))))
      .get
  }
  override val wordbankCodec = Codec(wordbankCodecName)

  override val wordbankSource: Source = {
    implicit val codec = wordbankCodec
    validSource(Source.fromFile(wordbankSrc))
      .get
  }
}
