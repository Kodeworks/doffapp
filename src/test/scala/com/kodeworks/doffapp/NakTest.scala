package com.kodeworks.doffapp

import java.io.File

import breeze.linalg.SparseVector
import epic.preprocess.MLSentenceSegmenter
import nak.NakContext._
import nak.cluster.Kmeans
import nak.core.IndexedClassifier
import nak.data._
import nak.liblinear.LiblinearConfig
import nak.util.ConfusionMatrix
import org.junit.Test

class NakTest {
  //  @Test
  def test {
    implicit val isoCodec = scala.io.Codec("ISO-8859-1")
    //    val indexer = new ExampleIndexer(true)
    val batchFeaturizer = new TfidfBatchFeaturizer[String](0)
    val trainingExamples: List[Example[String, String]] = fromLabeledDirs(new File("C:\\Users\\eirirlar\\Downloads\\20news-bydate\\20news-bydate-train")).toList
    //TODO apply stemmer pr language
    //TODO apply synonyms
    val tfidfFeaturized: Seq[Example[String, Seq[FeatureObservation[String]]]] = batchFeaturizer(trainingExamples)
    val leastSignificantWords: List[(String, Double)] = tfidfFeaturized.flatMap(_.features).groupBy(_.feature).mapValues(_.minBy(_.magnitude).magnitude).toList.sortBy(lm => -lm._2)
    val stopwords = leastSignificantWords.take(30).map(_._1).toSet
    //TODO manually go through stop words (admin of the systems task?)
    //TODO remove all crazy words like "======"
    //TODO spell correct
    val config = LiblinearConfig(cost = 5d, eps = .1d)
    val featurizer = new BowFeaturizer(stopwords)
    val classifier = trainClassifier(config, featurizer, trainingExamples)
    println("done training")

    // Evaluate
    println("Evaluating...")
    val evalDir = new File("C:\\Users\\eirirlar\\Downloads\\20news-bydate\\20news-bydate-test")
    val comparisons = for (ex <- fromLabeledDirs(evalDir).toList) yield
      (ex.label, classifier.predict(ex.features), ex.features)
    val (goldLabels, predictions, inputs) = comparisons.unzip3
    println(ConfusionMatrix(goldLabels, predictions, inputs))
    println("done")
  }
}
