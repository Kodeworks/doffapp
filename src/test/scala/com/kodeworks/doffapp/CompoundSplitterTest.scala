package com.kodeworks.doffapp

import com.kodeworks.doffapp.ctx.{Cfg, Files, Prop}
import com.kodeworks.doffapp.nlp.CompoundSplitter
import com.kodeworks.doffapp.nlp.wordbank.Wordbank
import org.junit.Test

class CompoundSplitterTest {
  @Test
  def test {
    val w0 = "tablettmisbrukeren"
    object Ctx extends Cfg with Prop with Files with Wordbank
    val cs = new CompoundSplitter(Ctx)
//    println(cs.splitN(w0))
    Ctx.wordbankWordsFull
      .foreach { w =>
        val split = cs.splitN(w)
        if (split.nonEmpty && split.exists(_.size > 2))
          println(w + ": " + split.mkString(","))
      }
  }
}
