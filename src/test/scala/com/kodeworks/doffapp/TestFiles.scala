package com.kodeworks.doffapp

import com.kodeworks.doffapp.ctx.{Cfg, Prop, Files}
import org.junit.{Assert, Test}
import Assert._

class TestFiles {
  @Test
  def testFiles {
    object files extends Cfg with Prop with Files
    assertEquals("i", files.mostUsedWords.head)
    assertEquals("phoenix", files.mostUsedWords.last)
  }

}
