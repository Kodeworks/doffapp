package com.kodeworks.doffapp

import com.kodeworks.doffapp.ctx.{Cfg, Files, Prop}
import com.kodeworks.doffapp.nlp.CompoundSplitter
import com.kodeworks.doffapp.nlp.wordbank.Wordbank
import org.junit.Test

class CompoundSplitterTest {
  @Test
  def test {
    val word = "konsulentstjenester"
    object Ctx extends Cfg with Prop with Files with Wordbank
    val cs = new CompoundSplitter(Ctx.wordbankWords, Map("*" -> "*"))
    Ctx.wordbankWords.take(100).foreach(word => println(word.full + ": " + cs.splitCompound(word.full).mkString))
  }
}
