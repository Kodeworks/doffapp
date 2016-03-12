package com.kodeworks.doffapp.nlp.kmeans

import breeze.linalg.{BroadcastedRows, *, DenseMatrix, DenseVector}
import breeze.stats
import com.kodeworks.doffapp.nlp.kmeans.Kmeans.Features
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import nak.cluster.{Kmeans => NakKmeans}

import scala.collection.immutable.IndexedSeq
import Kmeans._

import scala.collection.mutable.ListBuffer

/*
https://datasciencelab.wordpress.com/2014/01/21/selection-of-k-in-k-means-clustering-reloaded/
 */
class Kmeans(features: Features) {
  def fkAlphaDispersion(k: Int, dispersionOfKMinus1: Double = 0d, alphaOfKMinus1: Double = 1d): (Double, Double, Double) = {
    if (1 == k || 0d == dispersionOfKMinus1) (1d, 1d, 1d)
    else {
      val featureDimensions = features.headOption.map(_.size).getOrElse(1)
      val (dispersion, centroids) = new NakKmeans[DenseVector[Double]](features).run(k)
      val alpha =
        if (2 == k) 1d - 3d / (4d * featureDimensions)
        else alphaOfKMinus1 + (1d - alphaOfKMinus1) / 6d
      val fk = dispersion / (alpha * dispersionOfKMinus1)
      (fk, alpha, dispersion)
    }
  }

  def fks(maxK: Int) = {
    val fads = ListBuffer[(Double, Double, Double)](fkAlphaDispersion(1))
    var k = 2
    while (k <= maxK) {
      val (fk, alpha, dispersion) = fads(k - 2)
      fads += fkAlphaDispersion(k, dispersion, alpha)
      k += 1
    }
    fads.toList.map(_._1)
  }

  def detK = {
    val ks = (1 to maxK)

  }
}

object Kmeans {
  val maxK = 10
  type Features = IndexedSeq[DenseVector[Double]]
}
