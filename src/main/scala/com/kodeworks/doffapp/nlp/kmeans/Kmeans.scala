package com.kodeworks.doffapp.nlp.kmeans

import breeze.linalg.{*, DenseMatrix, DenseVector}
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation


/**
  * https://github.com/realstraw/abathur/blob/master/_abathur/cluster.py
  */
class Kmeans {
  type Features = Vector[DenseVector[Double]]
  val iterations = 10

  def determineMaxK(features: Features) = {
    math.round(math.sqrt(features.length / 2.0)).toInt
  }

  def determineK(features: Features) = {
    var maxCluster = 0
    val multiRun = 3
    def getJump(features: Features) = {
      if (maxCluster < 2) {
        maxCluster = determineMaxK(features)
      }
      val whitened = whiten(features)
    }
  }

  def whiten(features: Features) =
    DenseMatrix(features.map(_.toArray).toArray: _*).apply(::, *).map { col =>
      val _stddev = new StandardDeviation(false).evaluate(col.toArray)
      col / (if (0d == _stddev) 1d else _stddev)
    }
}
