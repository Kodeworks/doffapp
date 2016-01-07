package com.kodeworks.doffapp

import com.kodeworks.doffapp.ctx.{Cfg, Files, Prop}
import com.kodeworks.doffapp.nlp.CompoundSplitter
import com.kodeworks.doffapp.nlp.wordbank.Wordbank
import org.junit.Test

class CompoundSplitterTest {
  @Test
  def test {
    val w0 = "utvalgskriteria"
    object Ctx extends Cfg with Prop with Files with Wordbank
    val cs = new CompoundSplitter(Ctx)
    println(cs.splitCompound(w0))
    Ctx.wordbankWordsFull
      .foreach { w =>
        val split = cs.splitCompound(w)
        println(w + ": " + split.mkString(","))
      }
  }
}
