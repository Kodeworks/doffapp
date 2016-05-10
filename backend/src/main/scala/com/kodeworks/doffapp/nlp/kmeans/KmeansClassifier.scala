package com.kodeworks.doffapp.nlp.kmeans

import breeze.linalg.{sum, normalize, DenseVector}
import com.kodeworks.doffapp.nlp.kmeans.Kmeans.Features

import scala.collection.immutable.IndexedSeq
import KmeansClassifier._

/*
features: list of x y coords
labels: 1 or 0 for these coords
 */
class KmeansClassifier(features: IndexedSeq[DenseVector[Double]], labels: IndexedSeq[Int], maxK: Int = Kmeans.maxK) {
  val kmeans = new Kmeans(features)
  val (
    dispersion: Double,
    centroids: Features)
  = kmeans.detK
  val featuresPerCluster: Map[Int, IndexedSeq[Int]] = kmeans.clusterMap(centroids)
  val minLabel = labels.min
  val maxLabel = labels.max
  val labelCounts: IndexedSeq[Double] = countValues(labels, Some((minLabel, maxLabel)))

  def k: Int = centroids.size

  /*
  * - Do kmeans on whole classified features set, both 0s and 1s, including the new observation O
  * - find cluster C for O
  * - find amount of 1s compared to amount of 0s previously classified
  * - for each member of C except O, find result of function of distance to O
  *
  * */

  def classify(observation: DenseVector[Double]): Double = {
    val (nearests: IndexedSeq[Int], distances: IndexedSeq[Double]) =
      centroids.zipWithIndex.map {
        case (centroid, cid) => cid -> kmeans.distanceFun(observation, centroid)
      }.unzip
    def nearestLabels: IndexedSeq[IndexedSeq[Int]] = nearests.map(nearest => featuresPerCluster(nearest).map(labels(_)))

    /*
    For each cluster Q0 - Qn, count number of 1s and 0s.
    For the rest, unclassified observations, estimate number of 1s and 0s according to general prediction _without_ geographical consideration, call the number 1' and 0' for each cluster
    For all the 1s, do sum each Qn so that (Qn1 + Qn1'/7)/(7 * ln(distance(Qn)^2)), where number below dividing sign is 1 if distance = 0, else the given formula
    Same for 0s.
    prob(1) = count(1) / (count(1) +  count(0))
    prob(0) = count(0) / (count(1) + count(0))

     */

    def nearestLabelCounts: IndexedSeq[IndexedSeq[Double]] = nearestLabels.map(nearestLabel => countValues(nearestLabel, Some((minLabel, maxLabel))))

    val nearestLabelNormalizedCounts = nearestLabelCounts.map { counts =>
      val summed = counts.sum
      math.min(.9999, math.max(.0001, counts(1) / summed))
    }
    val distancesSummed = distances.sum
    val normalizedDistancesInverted = distances.map(distance => 1d - distance / distancesSummed)
    val distancesInvertedSummed = normalizedDistancesInverted.sum
    val normalizedDistancesInvertedNormalized = normalizedDistancesInverted.map(_ / distancesInvertedSummed)
    val distanceWeightedLabels: IndexedSeq[Double] = normalizedDistancesInverted.zip(nearestLabelNormalizedCounts).map(dc => dc._1 * dc._2)
    val classification = distanceWeightedLabels.sum
    classification
  }

}

object KmeansClassifier {
  // callers responsibility that there are no holes in values, i.e that we _dont_ see this: 0,0,0,1,5,5,6 (2,3,4 missing here)
  def countValues(vs: IndexedSeq[Int], range: Option[(Int, Int)] = None): IndexedSeq[Double] = {
    val counts: IndexedSeq[Double] = vs.groupBy(identity).mapValues(_.size.toDouble).toIndexedSeq.sortBy(_._1).map(_._2)
    if (range.isEmpty) counts
    else {
      val (min, max) = range.get
      (min to max).map { i =>
        if (counts.isDefinedAt(i)) counts(i)
        else 0d
      }
    }
  }
}