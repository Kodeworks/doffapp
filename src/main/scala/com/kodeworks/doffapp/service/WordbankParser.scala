package com.kodeworks.doffapp.service

import scala.util.parsing.combinator.RegexParsers

/*
http://download.savannah.gnu.org/releases/ordbanken/ordbanken-2013-02-17.tar.xz
fullform_bm.txt

wordFromLine returns Some("word") or None if a word could not be found on a line
 */
class WordbankParser extends RegexParsers {

  override def skipWhitespace = false

  val sep = """\t""".r
  val digits = """\d+""".r
  val word = opt("-" | "’") ~> """[-a-zA-Z\u00E6\u00C6\u00F8\u00D8\u00E5\u00D5]+""".r
  val any = """.*""".r
  val line = digits ~ sep ~ word ~ sep ~ word ~ sep ~ any ^^ {
    case _ ~ _ ~ _ ~ _ ~ word ~ _ ~ _ => word
  }

  def wordFromLine(input: String): Option[String] =
    parse(line, input) match {
      case Success(res, _) => Some(res)
      case x =>
//        println(x)
        None
    }
}

object WordbankParser extends WordbankParser
