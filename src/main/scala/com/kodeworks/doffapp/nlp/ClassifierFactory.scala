package com.kodeworks.doffapp.nlp

import com.kodeworks.doffapp.ctx.Nlp
import nak.NakContext
import nak.core.{IndexedClassifier, LiblinearClassifier}
import nak.data.{FeatureObservation, TfidfBatchFeaturizer, ExactFeatureMap, Example}


class ClassifierFactory(nlp: Nlp, trainingData: Seq[String]) {

  import nlp._

  private val examples0: Seq[Example[String, String]] = {
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
  private val bowClassifier0 = NakContext.trainClassifier(liblinearConfig, bowFeaturizer, examples0).asInstanceOf[LiblinearClassifier]
  private val lmap: Map[String, Int] = bowClassifier0.lmap
  private val fmap: Map[String, Int] = bowClassifier0.fmap.asInstanceOf[ExactFeatureMap].fmap

  def classifier(classifys: Seq[(String, String)]): Classifier =
    new Classifier {
      private val examples: Seq[Example[String, String]] = classifys.map { case (label, words) => Example(label, words) }
      private val bowClassifier = NakContext.trainClassifier(liblinearConfig, bowFeaturizer, examples).asInstanceOf[LiblinearClassifier]
      private val featurizer: Seq[Example[String, Seq[FeatureObservation[String]]]] = tfidfBatchFeaturizer(examples)
      private val map: Seq[Example[Int, Seq[FeatureObservation[Int]]]] = featurizer
        .map(_.map(_.map(_.map(bowClassifier0.indexOfFeature(_).get)).sortBy(_.feature)).relabel(bowClassifier0.indexOfLabel(_)))
      private val tfidfClassifier = NakContext.trainClassifier(liblinearConfig,
        map,
        lmap, fmap)

      override def bow(words: String): Map[String, Double] = {
        val x = bowClassifier.evalUnindexed(bowFeaturizer(words))
          .zipWithIndex.map { case (r, i) => bowClassifier0.labelOfIndex(i) -> r }.toMap
        x
      }

      override def tfidf(words: String): Map[String, Double] = {
        val x = tfidfClassifier.evalUnindexed(bowFeaturizer(words))
          .zipWithIndex.map { case (r, i) => bowClassifier0.labelOfIndex(i) -> r }.toMap
        x
      }
    }
}

trait Classifier {
  def bow(words: String): Map[String, Double]

  def tfidf(words: String): Map[String, Double]
}
