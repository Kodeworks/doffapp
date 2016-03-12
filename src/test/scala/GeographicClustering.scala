import breeze.linalg.DenseVector
import breeze.stats
import com.kodeworks.doffapp.nlp.kmeans.Kmeans
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

object GeographicClustering extends App {
  def makeGroup(min: Int, max: Int): Vector[DenseVector[Double]] =
    min to max flatMap (x => min to max map (x -> _)) map { case (x, y) => DenseVector(x.toDouble, y.toDouble) } toVector

  val points: Vector[DenseVector[Double]] = makeGroup(0, 2) ++ makeGroup(6, 7) ++ makeGroup(12, 13)
  println(points.map(_.toArray.mkString("[", ",", "]")).toArray.mkString("array([", ",", "])"))
  //  val points = Vector(
  //    DenseVector(1.9, 2.3),
  //    DenseVector(1.5, 2.5),
  //    DenseVector(0.8, 0.6),
  //    DenseVector(1.0, 1.0))
  //
  val kmeans = new Kmeans(points)
  val x = kmeans.fks(10)
  println(x)
  //  val (dispersion, centroids) = kmeans.run(5, 25)
  //  println("dispersion: " + dispersion)
  //  println("centroids : " + centroids)

}
