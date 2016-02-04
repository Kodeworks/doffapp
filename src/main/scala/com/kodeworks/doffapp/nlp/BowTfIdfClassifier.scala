package com.kodeworks.doffapp.nlp

import com.kodeworks.doffapp.ctx.Nlp
import nak.NakContext
import nak.core.LiblinearClassifier
import nak.data.{ExactFeatureMap, Example, FeatureObservation}

class BowTfIdfClassifier(nlp: Nlp, trainingData: Seq[(Int, String)]) {

  import nlp._

  //Bag of Words
  def toBowData(data: Seq[(Int, String)]) =
    data.map(t => Example(t._1.toString, t._2))

  val bowExamples = toBowData(trainingData)
  //TODO play with liblinearConfig
  val bowClassifier = NakContext.trainClassifier(liblinearConfig, bowFeaturizer, bowExamples)
  val lmap: Map[String, Int] = bowClassifier.asInstanceOf[LiblinearClassifier].lmap
  val fmap: Map[String, Int] = bowClassifier.asInstanceOf[LiblinearClassifier].fmap.asInstanceOf[ExactFeatureMap].fmap

  //TfIdf
  val tfidfExamples: Seq[Example[Int, Seq[FeatureObservation[Int]]]] =
    tfidfBatchFeaturizer(bowExamples).map(_.map(_.map(_.map(bowClassifier.indexOfFeature(_).get)).sortBy(_.feature)).relabel(bowClassifier.indexOfLabel(_)))

  val tfidfClassifier = NakContext.trainClassifier(liblinearConfig, tfidfExamples,
    lmap,
    fmap)
  //  val leastSignificantWords: List[(String, Double)] = tfidfFeaturized.flatMap(_.features).groupBy(_.feature).mapValues(_.minBy(_.magnitude).magnitude).toList.sortBy(lm => -lm._2)
  //  val stopwords: Set[String] = leastSignificantWords.take(30).map(_._1).toSet

  def apply(testData: Seq[(Int, String)]) = {
    val bowTest = toBowData(testData)
    bowTest.foreach { bt =>
      val f1 = bowClassifier.evalRaw(bt.features)
        .zipWithIndex.map { case (r, i) => bowClassifier.labelOfIndex(i) -> r }.toMap.toList.sortBy(_._1)
    }
//    val tfidfTest: Seq[Example[String, Seq[FeatureObservation[String]]]] = bowTest.map(_.map(bowFeaturizer(_)))
//    tfidfTest.foreach { tt =>
//      val r2 = tfidfClassifier.evalUnindexed(tt.features)
//        .zipWithIndex.map { case (r, i) => bowClassifier.labelOfIndex(i) -> r }.toMap.toList.sortBy(_._1)
//      println(r2.mkString(", "))
//    }

  }
}
