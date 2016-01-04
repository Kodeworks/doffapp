package com.kodeworks.doffapp

import com.kodeworks.doffapp.nlp.wordbank.{Parser, Word}
import com.typesafe.config.ConfigFactory
import org.junit.Assert._
import org.junit.Test

import scala.io.{Codec, Source}

class WordbankParserTest {
  @Test
  def test {
    val l0 = """************************************************************************"""
    val w0 = Parser.wordFromLine(l0)
    assertEquals(None, w0)

    val l1 = """* Copyright © 2006, 2007, 2008, 2009, 2010, 2011, 2012 The University  *"""
    val w1 = Parser.wordFromLine(l1)
    assertEquals(None, w1)

    val l2 = """* under the terms of the GNU General Public License as published by    *"""
    val w2 = Parser.wordFromLine(l2)
    assertEquals(None, w2)

    val l3 = """* along with this program.  If not, see <http://www.gnu.org/licenses/>.*"""
    val w3 = Parser.wordFromLine(l3)
    assertEquals(None, w3)

    val l4 = """156248	$	$	subst normert	000	0"""
    val w4 = Parser.wordFromLine(l4)
    assertEquals(None, w4)

    val l5 = """156550	’kke	’kke	adv unormert	695	0"""
    val w5 = Parser.wordFromLine(l5)
    assertEquals(Some(Word("kke", "kke", "adv")), w5)

    val l6 = """10	-akter	-akteren	subst mask appell ent be unormert	711	2"""
    val w6 = Parser.wordFromLine(l6)
    assertEquals(Some(Word("akter", "akteren", "subst")), w6)

    val l7 = """86622	AP-velger	AP-velgerne	subst mask appell fl be normert	712	4"""
    val w7 = Parser.wordFromLine(l7)
    assertEquals(Some("AP-velger", "AP-velgerne", "subst"), w7)

    val l8 = """396	AS	A.S	fork subst n\u00F8yt appell ent fl ub be @<SUBST unormert	000	0"""
    val w8 = Parser.wordFromLine(l8)
    assertEquals(None, w8)

    val l9 = """396	AS	A/S	fork subst n\u00F8yt appell ent fl ub be @<SUBST unormert	000	0"""
    val w9 = Parser.wordFromLine(l9)
    assertEquals(None, w9)

    val l10 = """86667	Arbeiderparti-byr\u00E5d	Arbeiderparti-byr\u00E5d	subst mask appell ent ub normert	700	1"""
    val w10 = Parser.wordFromLine(l10)
    assertEquals(Some(Word("Arbeiderparti-byr\u00E5d", "Arbeiderparti-byr\u00E5d", "subst")), w10)
  }

  @Test
  def testWordbankParser {
    val config = ConfigFactory.load
    implicit val codec = Codec(config.getString("wordbank.codec"))
    val words = Source.fromFile(config.getString("wordbank.src")).getLines.toList.flatMap(Parser.wordFromLine _)
    assertEquals(Word("A", "A", "adv"), words.head)
    assertEquals(Word("\u00E5vokster", "\u00E5vokstrene", "subst"), words.last)
  }
}
