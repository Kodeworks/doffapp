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
    val cs = new CompoundSplitter(Ctx, Map("*" -> "*"))
    println(cs.splitCompound("bedriftshelsetjeneste"))
    Ctx.wordbankWords.foreach { w =>
      val split = cs.splitCompound(w.full)
      println(w.full + ": " + split.mkString(","))
    }
  }
}
