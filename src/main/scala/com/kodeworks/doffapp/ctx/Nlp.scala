package com.kodeworks.doffapp.ctx

import com.kodeworks.doffapp.nlp.{BowTfIdfFeaturizer, MostUsedWords}
import nak.classify.NaiveBayes
import nak.data.TfidfBatchFeaturizer
import nak.liblinear.LiblinearConfig

trait Nlp {
  this: MostUsedWords =>
  println("Loading Nlp")

  val liblinearConfig = LiblinearConfig(cost = 5d, eps = .1)
  val tfidfBatchFeaturizer = new TfidfBatchFeaturizer[String](0, mostUsedWordsTop64)
  val bowFeaturizer = new BowTfIdfFeaturizer(mostUsedWordsTop64)
  val naiveBayes = new NaiveBayes.Trainer[Boolean, String]()
  val classifyLabels = Array("0", "1")
}
