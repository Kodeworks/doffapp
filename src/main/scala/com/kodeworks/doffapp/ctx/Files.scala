package com.kodeworks.doffapp.ctx

import com.kodeworks.doffapp.nlp.wordbank.{Parser, Word}

import scala.io.{Codec, Source}
import scala.util.Try
import scala.util.parsing.combinator.RegexParsers

//TODO limit responsibility of this file to make sure file is at correct location, delegate parsing to elsewhere
trait Files {
  this: Prop =>
  val mostUsedWords: Set[String] = {
    implicit val codec = Codec(mostUsedWordsCodec)
    def fromSource(src: => Source): Option[List[String]] =
      Try {
        try {
          object parser extends RegexParsers {
            override def skipWhitespace: Boolean = false

            val count = opt( """\s+""".r) ~> """\d+""".r
            val word = """[a-zA-Z\u00E6\u00C6\u00F8\u00D8\u00E5\u00D5]+""".r
            val line = count ~ " " ~> word

            def parseLine(l: String) =
              parse(line, l).map(Some(_)).getOrElse(None)
          }
          src.getLines.flatMap(parser.parseLine _).toList
        } finally src.close
      }.toOption
    fromSource(Source.fromFile(mostUsedWordsSrc))
      .orElse(fromSource(Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(mostUsedWordsSrc))))
      .getOrElse(Nil).toSet
  }

  val wordbankWords: List[Word] = {
    implicit val codec = Codec(wordbankCodec)
    def fromSource(src: => Source): Option[List[Word]] =
      Try {
        try src.getLines.toList.flatMap(Parser.wordFromLine _)
        finally src.close
      }.toOption
    Source.fromFile(wordbankSrc).getLines.toList.flatMap(Parser.wordFromLine _)
  }

  val wordbankDict: Map[String, Int] = wordbankWords.map(_.full -> Int.MaxValue).toMap
}
