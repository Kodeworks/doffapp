package com.kodeworks.doffapp.ctx

import nak.data.TfidfBatchFeaturizer
import nak.liblinear.LiblinearConfig

trait Nlp {
  this: Ctx =>
  println("Loading Nlp")

  val liblinerConfig = LiblinearConfig(cost = 5d, eps = .1d)
  val batchFeaturizer = new TfidfBatchFeaturizer[String](0, Ctx.mostUsedWords.take(64).toSet)
}
