package com.kodeworks.doffapp.nlp

import breeze.linalg.{DenseVector, DenseMatrix, Counter}
import breeze.stats
import com.kodeworks.doffapp.ctx.Nlp
import com.kodeworks.doffapp.model.Classification
import nak.NakContext
import nak.classify.NaiveBayes
import nak.core.LiblinearClassifier
import nak.data.{ExactFeatureMap, Example, FeatureObservation}


class ClassifierFactory(nlp: Nlp, trainingData: Seq[String]) {

  import nlp._

  type LabelWords = (String, String)

  private def examples0: Seq[Example[String, String]] = {
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
    }) :+ Example(classifyLabels(inc), "DEFAULT")
  }

  private val bowClassifier0 = NakContext.trainClassifier(liblinearConfig, bowFeaturizer, examples0).asInstanceOf[LiblinearClassifier]
  private val lmap: Map[String, Int] = bowClassifier0.lmap
  private val fmap: Map[String, Int] = bowClassifier0.fmap.asInstanceOf[ExactFeatureMap].fmap


  def unweightedClassifier(classifys: Seq[LabelWords]): UnweightedClassifier = {
    new UnweightedClassifier {
      private val examples: Seq[Example[String, String]] = classifys.map { case (label, words) => Example(label, words) }
      private val bowClassifier = NakContext.trainClassifier(liblinearConfig, bowFeaturizer, examples).asInstanceOf[LiblinearClassifier]
      private val featurizer: Seq[Example[String, Seq[FeatureObservation[String]]]] = tfidfBatchFeaturizer(examples)
      private val map: Seq[Example[Int, Seq[FeatureObservation[Int]]]] = featurizer
        .map(_.map(_.map(_.map(bowClassifier0.indexOfFeature(_).get)).sortBy(_.feature)).relabel(bowClassifier0.indexOfLabel(_)))
      private val tfidfClassifier = NakContext.trainClassifier(liblinearConfig,
        map,
        lmap, fmap)

      def toNbData(data: Seq[LabelWords]): Seq[Example[String, Counter[String, Double]]] =
        data.map(t => Example(t._1, Counter.count(t._2.split(" "): _*).mapValues(_.toDouble)))

      val nbExamples = toNbData(classifys)
      val nbClassifier: NaiveBayes[String, String] = naiveBayes.train(nbExamples)

      private def get1(map: Map[String, Double]) =
        if (map.contains("1")) map("1")
        else if (map.contains("0)")) 1d - map("0")
        else 0d

      override def bow(words: String): Double = {
        get1(bowClassifier.evalUnindexed(bowFeaturizer(words))
          .zipWithIndex.map { case (r, i) => bowClassifier0.labelOfIndex(i) -> r }.toMap)
      }

      override def tfidf(words: String): Double = {
        get1(tfidfClassifier.evalUnindexed(bowFeaturizer(words))
          .zipWithIndex.map { case (r, i) => bowClassifier0.labelOfIndex(i) -> r }.toMap)
      }

      override def nb(words: String): Double = {
        get1(nbClassifier.normScores(Counter.count(words.split(" "): _*).mapValues(_.toDouble)))
      }
    }
  }

  def newClassifier(classifys: Seq[LabelWords], _weights: Array[Double]): Classifier =
    new Classifier {
      private val unweighted: UnweightedClassifier = unweightedClassifier(classifys)

      override def bow(words: String): Double = unweighted.bow(words)

      override def tfidf(words: String): Double = unweighted.tfidf(words)

      override def nb(words: String): Double = unweighted.nb(words)

      override def weighted(words: String): Double = {
        def predicts = Array(
          bow(words),
          tfidf(words),
          nb(words))
        weightedMean(predicts, weights)
      }

      override def weights: Array[Double] = _weights

      override def mean(words: String) =
        stats.mean(DenseVector(
          bow(words),
          tfidf(words),
          nb(words)))

      override def classification(tender: String, user: String, words: String): Classification = {
        def predicts = Array(
          bow(words),
          tfidf(words),
          nb(words))
        Classification(tender, user, predicts(0), predicts(1), predicts(2), weightedMean(predicts, weights), stats.mean(DenseVector(predicts)))
      }
    }

  def trainWeights(classifys: Seq[LabelWords]): Array[Double] = {
    if (classifys.size < 2) (0 to 2).toArray.map(_ => .33333333)
    else {
      //TODO make parallell?
      def xvalBase: Seq[(LabelWords, Seq[LabelWords])] = classifys.zipWithIndex.map {
        case (c, i) => c -> (classifys.take(i) ++ classifys.drop(i + 1))
      }
      val (labels: Array[Double], predictions: Array[Array[Double]]) = xvalBase.map {
        case (test, train) =>
          val cls = unweightedClassifier(train)
          test._1.toDouble -> Array(
            cls.bow(test._2),
            cls.tfidf(test._2),
            cls.nb(test._2))
      }.toArray.unzip
      val weights = weighLabeledPredictions(DenseMatrix(predictions: _*), DenseVector(labels)).toArray
      weights
    }
  }

  def classifier(classifys: Seq[LabelWords]): Classifier = {
    newClassifier(classifys, trainWeights(classifys))
  }
}

sealed trait UnweightedClassifier {
  def bow(words: String): Double

  def tfidf(words: String): Double

  def nb(words: String): Double
}

sealed trait Classifier extends UnweightedClassifier {
  override def bow(words: String): Double

  override def tfidf(words: String): Double

  override def nb(words: String): Double

  def mean(words: String): Double

  def weighted(words: String): Double

  def classification(tender: String, user: String, words: String): Classification

  def weights: Array[Double]
}