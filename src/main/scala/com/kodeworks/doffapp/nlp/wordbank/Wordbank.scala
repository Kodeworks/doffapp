package com.kodeworks.doffapp.nlp.wordbank

import com.kodeworks.doffapp.ctx.{Cfg, Files, Prop}

import scala.io.Source
import scala.util.Try

trait Wordbank extends Cfg with Prop with Files {
  println("Loading Wordbank")
  val wordbankWords: List[Word] = {
    implicit val codec = wordbankCodec
    def fromSource(src: => Source): Option[List[Word]] =
      Try {
        try src.getLines.toList.flatMap(Parser.wordFromLine _)
        finally src.close
      }.toOption
    Source.fromFile(wordbankSrc).getLines.flatMap(Parser.wordFromLine _).toList.distinct
  }
  val wordbankWordsFull = wordbankWords.map(_.full).distinct
  val wordbankDict: Map[String, Int] = wordbankWords.map(_.full -> Int.MaxValue).toMap
  //  val wordbankWordsFullToBase: Map[String, String] = wordbankWords.map(w => w.full -> w.base).toMap
  val wordbankWordsFullToBases: Map[String, List[String]] = wordbankWords.groupBy(_.full).map(w => w._1 -> w._2.map(_.base))
  val wordbankWordsBaseToFulls: Map[String, List[String]] = wordbankWords.groupBy(_.base).map(w => w._1 -> w._2.map(_.full))

  def wordbankWordsFullToBase(word: String) =
    wordbankWordsFullToBases.get(word).map {
      case bases if bases.contains(word) => word
      case base :: _ => base
      case _ => word
    }.getOrElse(word)
}
