package com.kodeworks.doffapp.ctx

import com.kodeworks.doffapp.nlp.{BowTfIdfFeaturizer, MostUsedWords}
import nak.classify.NaiveBayes
import nak.classify.NaiveBayes.Trainer
import nak.data.TfidfBatchFeaturizer
import nak.liblinear.LiblinearConfig

trait Nlp {
   val liblinearConfig: LiblinearConfig
   val tfidfBatchFeaturizer: TfidfBatchFeaturizer[String]
   val bowFeaturizer: BowTfIdfFeaturizer
   val naiveBayes: Trainer[Boolean, String]
   val classifyLabels: Array[String]
}

trait NlpImpl extends Nlp{
  this: MostUsedWords =>
  println("Loading Nlp")
  override val liblinearConfig = LiblinearConfig(cost = 5d, eps = .1)
  override val tfidfBatchFeaturizer = new TfidfBatchFeaturizer[String](0, mostUsedWordsTop64)
  override val bowFeaturizer = new BowTfIdfFeaturizer(mostUsedWordsTop64)
  override val naiveBayes = new NaiveBayes.Trainer[Boolean, String]()
  override val classifyLabels = Array("0", "1")
}
