package com.kodeworks.doffapp.nlp.wordbank

import com.kodeworks.doffapp.ctx.{Cfg, Files, Prop}

import scala.io.Source
import scala.util.Try

trait Wordbank extends Cfg with Prop with Files {
  val wordbankWords: List[Word] = {
    implicit val codec = wordbankCodec
    def fromSource(src: => Source): Option[List[Word]] =
      Try {
        try src.getLines.toList.flatMap(Parser.wordFromLine _)
        finally src.close
      }.toOption
    Source.fromFile(wordbankSrc).getLines.flatMap(Parser.wordFromLine _).toList.distinct
  }

  val wordbankDict: Map[String, Int] = wordbankWords.map(_.full -> Int.MaxValue).toMap
  val wordbankWordsByFull: Map[String, List[Word]] = wordbankWords.groupBy(_.full)
}
