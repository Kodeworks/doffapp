package com.kodeworks.doffapp

import breeze.generic.{MappingUFunc, UFunc}
import breeze.linalg._
import breeze.numerics.sqrt._
import nak.data.Example
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

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
    def sqErrs: DenseMatrix[Double] = DenseMatrix.horzcat(predictions, labels.asDenseMatrix.t).apply(*, ::).map { row =>
      val l: Double = row(-1)
      val ps: DenseVector[Double] = row(0 to -2)
      ps.map(p => math.pow(l - p, 2d))
    }
    def sumSqErrs = sqErrs(::, *).map(sum(_)).inner
    def normSumSqErrs = normalize(sumSqErrs)
    def normInvNormSumSqErrs = {
      val invNormSumSqErrs = normSumSqErrs.map(1d - _)
      val sm = sum(invNormSumSqErrs)
      invNormSumSqErrs.map(_ / sm)
    }
    normInvNormSumSqErrs
  }

  def weightedMean(values: Array[Double], weights: Array[Double]): Double = {
    values.zip(weights).map(pv => pv._1 * pv._2).sum
  }

  def whiten(features: Vector[DenseVector[Double]]): DenseMatrix[Double] =
    DenseMatrix(features.map(_.toArray).toArray: _*).apply(::, *).map { col =>
      val _stddev = new StandardDeviation(false).evaluate(col.toArray)
      col / (if (0d == _stddev) 1d else _stddev)
    }
}