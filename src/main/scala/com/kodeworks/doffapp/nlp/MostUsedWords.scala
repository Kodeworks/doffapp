package com.kodeworks.doffapp.nlp

import com.kodeworks.doffapp.ctx.{Cfg, Files, Prop}

import scala.io.Source
import scala.util.Try
import scala.util.parsing.combinator.RegexParsers

trait MostUsedWords extends Cfg with Prop with Files {
  val mostUsedWords: Set[String] = {
    implicit val codec = mostUsedWordsCodec
    def fromSource(src: => Source): Option[List[String]] =
      Try(
        try {
          object parser extends RegexParsers {
            override def skipWhitespace: Boolean = false

            val count = opt( """\s+""".r) ~> """\d+""".r
            val word = """[a-zA-Z\u00E6\u00C6\u00F8\u00D8\u00E5\u00D5]+""".r
            val line = count ~ " " ~> word

            def parseLine(l: String) =
              parse(line, l).map(Some(_)).getOrElse(None)
          }
          Some(src.getLines.flatMap(parser.parseLine _).toList)
        } finally src.close
      ).toOption.flatten
    fromSource(mostUsedWordsSource).getOrElse(Nil).toSet
  }
}
