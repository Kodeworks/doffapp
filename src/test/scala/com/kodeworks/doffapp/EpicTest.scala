package com.kodeworks.doffapp

import epic.preprocess.MLSentenceSegmenter
import org.junit.Test

class EpicTest {
  @Test
  def test {
    val text = "So this is christmas? I hope I get many, many presents. And I'd really appreciate if it snowed"
    val sentenceSplitter = MLSentenceSegmenter.bundled().get
    val tokenizer = new epic.preprocess.TreebankTokenizer()

    val sentences: IndexedSeq[IndexedSeq[String]] = sentenceSplitter(text).map(tokenizer).toIndexedSeq
    val parser = epic.models.ParserSelector.loadParser("en").get // or another 2 letter code.
    val tagger = epic.models.PosTagSelector.loadTagger("en").get // or another 2 letter code.
    val ner = epic.models.NerSelector.loadNer("en").get// or another 2 letter code.

    for(sentence <- sentences) {
      val tree = parser(sentence)
      println(tree.render(sentence))

      val tags = tagger.bestSequence(sentence)
      println(tags.render)

      val segments = ner.bestSequence(sentence)
      println(segments.render)
    }
  }
}
