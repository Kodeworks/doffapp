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
  def fkAlphaDispersionCentroids(k: Int, dispersionOfKMinus1: Double = 0d, alphaOfKMinus1: Double = 1d): (Double, Double, Double, Features) = {
    if (1 == k || 0d == dispersionOfKMinus1) (1d, 1d, 1d, Vector.empty)
    else {
      val featureDimensions = features.headOption.map(_.size).getOrElse(1)
      val (dispersion, centroids: Features) = new NakKmeans[DenseVector[Double]](features).run(k)
      val alpha =
        if (2 == k) 1d - 3d / (4d * featureDimensions)
        else alphaOfKMinus1 + (1d - alphaOfKMinus1) / 6d
      val fk = dispersion / (alpha * dispersionOfKMinus1)
      (fk, alpha, dispersion, centroids)
    }
  }

  def fks(maxK: Int = maxK): List[(Double, Double, Double, Features)] = {
    val fadcs = ListBuffer[(Double, Double, Double, Features)](fkAlphaDispersionCentroids(1))
    var k = 2
    while (k <= maxK) {
      val (fk, alpha, dispersion, features) = fadcs(k - 2)
      fadcs += fkAlphaDispersionCentroids(k, dispersion, alpha)
      k += 1
    }
    fadcs.toList
  }

  /*
  K, dispersion, centroids
   */
  def detK: (Int, Double, Features) = {
    val vals = fks().zipWithIndex.minBy(_._1._1)
    (vals._2 + 1, vals._1._3, vals._1._4)
  }
}

object Kmeans {
  val maxK = 10
  type Features = IndexedSeq[DenseVector[Double]]
}
