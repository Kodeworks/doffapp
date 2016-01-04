package com.kodeworks.doffapp.nlp.wordbank

import scala.util.parsing.combinator.RegexParsers

/*
http://download.savannah.gnu.org/releases/ordbanken/ordbanken-2013-02-17.tar.xz
fullform_bm.txt

wordFromLine returns Some("word") or None if a word could not be found on a line
 */
class Parser extends RegexParsers {

  override def skipWhitespace = false

  val sep = """\t""".r
  val digits = """\d+""".r
  val word = opt("-" | "’") ~> """[-a-zA-Z\u00E6\u00C6\u00F8\u00D8\u00E5\u00D5]+""".r
  val any = """.*""".r
  val line = digits ~ sep ~ word ~ sep ~ word ~ sep ~ word ~ any ^^ {
    case _ ~ _ ~ base ~ _ ~ full ~ _ ~ clazz ~ _ => Word(base, full, clazz)
  }

  def wordFromLine(input: String): Option[Word] =
    parse(line, input) match {
      case Success(res, _) => Some(res)
      case x =>
        //        println(x)
        None
    }
}

object Parser extends Parser
