package com.kodeworks.doffapp

import com.kodeworks.doffapp.nlp.SpellingCorrector
import org.junit.{Assert, Test}
import Assert._

class SpellingCorrectorTest {
  val sp = new SpellingCorrector(Map("saftig" -> 4, "saftige" -> 5))

  @Test
  def testOneEditBeatsTwoEdits {
    val corrected = sp.correct("sfatig")
    assertEquals("saftig", corrected)

    val corrected1 = sp.correct("sfatige")
    assertEquals("saftige", corrected1)
  }

  @Test
  def testThreeEditsReturnsSame {
    val corrected = sp.correct("sfaty")
    assertEquals("sfaty", corrected)
  }
}
