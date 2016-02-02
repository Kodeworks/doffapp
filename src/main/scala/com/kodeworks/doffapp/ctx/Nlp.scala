package com.kodeworks.doffapp.ctx

import nak.classify.NaiveBayes
import nak.data.{BowFeaturizer, TfidfBatchFeaturizer}
import nak.liblinear.LiblinearConfig

trait Nlp {
  this: Ctx =>
  println("Loading Nlp")

  val liblinerConfig = LiblinearConfig(cost = 5d, eps = .1)
  val tfidfBatchFeaturizer = new TfidfBatchFeaturizer[String](0, mostUsedWordsTop64)
  val bowFeaturizer = new BowFeaturizer(mostUsedWordsTop64)
  val naiveBayes = new NaiveBayes.Trainer[Boolean,String]()
}
