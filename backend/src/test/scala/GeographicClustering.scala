import breeze.linalg.DenseVector
import breeze.stats
import com.kodeworks.doffapp.nlp.kmeans.{KmeansClassifier, Kmeans}
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

object GeographicClustering extends App {
  def makeGroup(min: Int, max: Int): Vector[DenseVector[Double]] =
    min to max flatMap (x => min to max map (x -> _)) map { case (x, y) => DenseVector(x.toDouble, y.toDouble) } toVector

  //    val train = makeGroup(0, 2) ++ makeGroup(6, 7) ++ makeGroup(12, 13)
  val (_train, labels) = Vector(
    7040762 -> 272144 -> 1,
    7040761 -> 272143 -> 1,
    7040760 -> 272142 -> 1,
    7040759 -> 272141 -> 0,
    7040758 -> 272140 -> 1,
    6654147 -> 261177 -> 0,
    6654146 -> 261176 -> 0,
    6603810 -> 43970 -> 1,
    6603811 -> 43971 -> 0,
    6603812 -> 43972 -> 0,
    6603813 -> 43973 -> 0
  ).unzip
  val train = _train.map(c => DenseVector(c._1.toDouble, c._2.toDouble))
  println("train size: " + train.size)

//  val test = DenseVector(6998292d, 261975d)
  val test2 = DenseVector(7040761d, 272143d)

  val kc = new KmeansClassifier(train, labels)
  println("k: " + kc.k)
  println("centroids: " + kc.centroids)
  println("clusterMap: " + kc.featuresPerCluster)
//  kc.classify(test)
  println("classify: " + kc.classify(test2))

}
