package com.kodeworks.doffapp

import breeze.linalg._
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

  def weighLabeledPredictions(predictions: DenseMatrix[Double], labels: DenseVector[Double]): DenseVector[Double] = {
    val sqErrs: DenseMatrix[Double] = DenseMatrix.horzcat(predictions, labels.asDenseMatrix.t).apply(*, ::).map { row =>
      val l: Double = row(-1)
      val ps: DenseVector[Double] = row(0 to -2)
      ps.map(p => math.pow(l - p, 2))
    }
    val sumSqErrs = sqErrs(::, *).map(sum(_)).inner
    val normSumSqErrs = normalize(sumSqErrs)
    val invNormSumSqErrs = normSumSqErrs.map(1d - _)
    val normInvNormSumSqErrs = {
      val sm = sum(invNormSumSqErrs)
      invNormSumSqErrs.map(_ / sm)
    }
    normInvNormSumSqErrs
  }

  def predictionsWeightedMean(predictions: DenseVector[Double], weights: DenseVector[Double]): Double = {
    predictions.toArray.zip(weights.toArray).map(pv => pv._1 * pv._2).sum / predictions.size
  }
}