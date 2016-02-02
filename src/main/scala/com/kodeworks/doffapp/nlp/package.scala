package com.kodeworks.doffapp

import breeze.linalg.Counter
import nak.data.Example

package object nlp {
  def countString(raw: String, stopwords: Set[String] = Set[String]()): Counter[String, Double] =
    Counter.count(raw
      .replaceAll("""([\?!\";\|\[\].,'])""", " $1 ")
      .trim
      .split("\\s+")
      .filterNot(stopwords): _*
    ).mapValues(_.toDouble)

  def countStringExamples[L](examples: Iterable[Example[L, String]], stopwords: Set[String] = Set[String]()): Seq[Example[L, Counter[String, Double]]] =
    examples.map(_.map(countString(_, stopwords))).toSeq
}