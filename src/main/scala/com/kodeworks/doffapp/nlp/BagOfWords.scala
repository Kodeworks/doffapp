package com.kodeworks.doffapp.nlp

import com.kodeworks.doffapp.ctx.Nlp
import nak.NakContext
import nak.core.LiblinearClassifier
import nak.data.{ExactFeatureMap, Example}


class BagOfWords(val nlp: Nlp, trainingData: Seq[String]) {

  import nlp._

  val examples = {
    var i = -1
    def inc: Int = {
      i += 1
      if (classifyLabels.size == i)
        i = 0
      i
    }
    trainingData.map(t => {
      val labels = classifyLabels(inc)
      Example(labels.toString, t)
    })
  }
  val classifier = NakContext.trainClassifier(liblinearConfig, bowFeaturizer, examples)
  val lmap: Map[String, Int] = classifier.asInstanceOf[LiblinearClassifier].lmap
  val fmap: Map[String, Int] = classifier.asInstanceOf[LiblinearClassifier].fmap.asInstanceOf[ExactFeatureMap].fmap
}
