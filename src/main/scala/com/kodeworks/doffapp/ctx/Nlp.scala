package com.kodeworks.doffapp.ctx

import com.kodeworks.doffapp.nlp.{BowTfIdfFeaturizer, MostUsedWords}
import nak.classify.NaiveBayes
import nak.data.TfidfBatchFeaturizer
import nak.liblinear.LiblinearConfig

trait Nlp {
   val liblinearConfig: LiblinearConfig
   val tfidfBatchFeaturizer: TfidfBatchFeaturizer[String]
   val bowFeaturizer: BowTfIdfFeaturizer
   val naiveBayes: NaiveBayes.Trainer[String, String]
   val classifyLabels: Array[String]
}

trait NlpImpl extends Nlp{
  this: MostUsedWords =>
  println("Loading Nlp")
  override val liblinearConfig = LiblinearConfig(cost = 5d, eps = .1)
  //TODO skip stopwords for now, increases number of unclassifiable tenders to unreasonable numbers (375 -> 890)
  //consequence unknown, maybe we can utilize it
  override val tfidfBatchFeaturizer = new TfidfBatchFeaturizer[String](0)//, mostUsedWordsTop64)
  override val bowFeaturizer = new BowTfIdfFeaturizer()//mostUsedWordsTop64)
  override val naiveBayes = new NaiveBayes.Trainer[String, String]()
  override val classifyLabels = Array("0", "1")
}
