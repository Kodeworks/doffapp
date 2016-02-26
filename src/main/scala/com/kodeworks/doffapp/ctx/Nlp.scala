package com.kodeworks.doffapp.ctx

import com.kodeworks.doffapp.nlp.{BowTfIdfFeaturizer, MostUsedWords}
import nak.classify.NaiveBayes
import nak.data.TfidfBatchFeaturizer
import nak.liblinear.{SolverType, LiblinearConfig}

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
  override val liblinearConfig = LiblinearConfig(cost = 5d, eps = .1, solverType = SolverType.L1R_LR, showDebug = true)
  //TODO skip stopwords for now, increases number of unclassifiable tenders to unreasonable numbers (375 -> 890)
  //consequence unknown, maybe we can utilize it
  override val tfidfBatchFeaturizer = new TfidfBatchFeaturizer[String](0, Set(), true)//, mostUsedWordsTop64) //TODO check if add defualt makes any difference
  override val bowFeaturizer = new BowTfIdfFeaturizer()//mostUsedWordsTop64)
  override val naiveBayes = new NaiveBayes.Trainer[String, String]()
  override val classifyLabels = Array("0", "1")
}
