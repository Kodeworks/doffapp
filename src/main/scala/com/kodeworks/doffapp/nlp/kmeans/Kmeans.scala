package com.kodeworks.doffapp.nlp.kmeans

import breeze.linalg.DenseVector
import com.kodeworks.doffapp.nlp.kmeans.Kmeans.{Features, _}
import nak.cluster.{Kmeans => NakKmeans}

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable.ListBuffer

/*
https://datasciencelab.wordpress.com/2014/01/21/selection-of-k-in-k-means-clustering-reloaded/
 */
class Kmeans(features: Features,
             maxK: Int = maxK,
             val distanceFun: (DenseVector[Double], DenseVector[Double]) => Double = NakKmeans.euclideanDistance) {
  private val clampedMaxK = math.min(maxK, features.size)
  val nakKmeans: NakKmeans[DenseVector[Double]] = new NakKmeans[DenseVector[Double]](features, distanceFun)

  def fkAlphaDispersionCentroids(k: Int, dispersionOfKMinus1: Double = 0d, alphaOfKMinus1: Double = 1d): (Double, Double, Double, Features) = {
    val (dispersion, centroids: Features) = nakKmeans.run(k)
    if (1 == k || 0d == dispersionOfKMinus1) (1d, 0d, dispersion, centroids)
    else {
      val featureDimensions = features.headOption.map(_.size).getOrElse(1)
      val alpha =
        if (2 == k) 1d - 3d / (4d * featureDimensions)
        else alphaOfKMinus1 + (1d - alphaOfKMinus1) / 6d
      val fk = dispersion / (alpha * dispersionOfKMinus1)
      (fk, alpha, dispersion, centroids)
    }
  }

  def fadcs: List[(Double, Double, Double, Features)] = {
    val fadcs = ListBuffer[(Double, Double, Double, Features)](fkAlphaDispersionCentroids(1))
    var k = 2
    while (k <= clampedMaxK) {
      val (_, alpha, dispersion, _) = fadcs(k - 2)
      fadcs += fkAlphaDispersionCentroids(k, dispersion, alpha)
      k += 1
    }
    fadcs.toList
  }

  //TODO find weight of each of the centroids
  /*
  returns dispersion, centroids
  K is implicitly given as the size of centroids
   */
  def detK: (Double, Features) = {
    val _fadcs: List[(Double, Double, Double, Features)] = fadcs
    val heads =
      if (1 == _fadcs.length) _fadcs
      else _fadcs.take(_fadcs.length - 1)
    val vals = heads.minBy(_._1)
    (vals._3, vals._4)
  }

  /*
  returns map of centroid index to feature index
   */
  def clusterMap(centroids: Features): Map[Int, IndexedSeq[Int]] =
    nakKmeans.computeClusterMemberships(centroids)._2.zipWithIndex.groupBy(_._1).mapValues(_.map(_._2))
}

object Kmeans {
  val maxK = 10
  type Features = IndexedSeq[DenseVector[Double]]
}
