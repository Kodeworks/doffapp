package com.kodeworks.doffapp.nlp

import nak.data.{FeatureObservation, Featurizer}


class BowTfIdfFeaturizer(stopwords: Set[String] = Set[String]()) extends Featurizer[String, String] {
  def apply(raw: String) = raw
    .replaceAll("""([\?!\";\|\[\].,'])""", " $1 ")
    .trim
    .split("\\s+")
    .filterNot(stopwords)
    .map(FeatureObservation(_))
}
