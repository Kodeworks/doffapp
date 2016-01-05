package com.kodeworks.doffapp

import com.kodeworks.doffapp.ctx.{Cfg, Prop, Files}
import com.kodeworks.doffapp.nlp.MostUsedWords
import org.junit.{Assert, Test}
import Assert._

class TestFiles {
  @Test
  def testFiles {
    object Files extends Cfg with Prop with Files with MostUsedWords
    assertEquals("i", Files.mostUsedWords.head)
    assertEquals("phoenix", Files.mostUsedWords.last)
  }

}
