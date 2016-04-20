package com.kodeworks.doffapp

import com.kodeworks.doffapp.ctx._
import com.kodeworks.doffapp.nlp.CompoundSplitter
import com.kodeworks.doffapp.nlp.wordbank.{WordbankImpl, Wordbank}
import org.junit.Test

class CompoundSplitterTest {
  @Test
  def test {
    val w0 = "tablettmisbrukeren"
    object Ctx extends CfgImpl with PropImpl with FilesImpl with WordbankImpl
    val cs = new CompoundSplitter(Ctx)
//    println(cs.splitN(w0))
    Ctx.wordbankWordsFull
      .foreach { w =>
        val split = cs.splitNShortest(w)
        if (split.nonEmpty
        //  && split.exists(_.size > 2)
        )
          println(w + ": " + split.mkString(","))
      }
  }
}
