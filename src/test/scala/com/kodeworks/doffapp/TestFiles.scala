package com.kodeworks.doffapp

import com.kodeworks.doffapp.ctx._
import com.kodeworks.doffapp.nlp.MostUsedWords
import com.kodeworks.doffapp.nlp.wordbank.WordbankImpl
import org.junit.{Assert, Test}
import Assert._

class TestFiles {
  @Test
  def testFiles {
    object Files extends CfgImpl with PropImpl with FilesImpl with WordbankImpl with MostUsedWords
    assertEquals("i", Files.mostUsedWords.head)
    assertEquals("phoenix", Files.mostUsedWords.last)
  }

}
