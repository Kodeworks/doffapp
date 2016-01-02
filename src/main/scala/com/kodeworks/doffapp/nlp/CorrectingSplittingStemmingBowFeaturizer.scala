package com.kodeworks.doffapp.nlp

import nak.data.{FeatureObservation, Featurizer}

class CorrectingSplittingStemmingBowFeaturizer(correctionFrequencies: Map[String, Int], stopwords: Set[String] = Set[String]()) extends Featurizer[String, String] {
  val sp = new SpellingCorrector(correctionFrequencies)

  def apply(raw: String) = {
    raw
      .replaceAll( """([\?!\";\|\[\].,'])""", " $1 ")
      .trim
      .split("\\s+")
      .filterNot(stopwords)
      .map(tok => FeatureObservation("word=" + tok))
  }
}
