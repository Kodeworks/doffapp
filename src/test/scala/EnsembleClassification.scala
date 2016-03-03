import breeze.linalg._
import com.kodeworks.doffapp.nlp

object EnsembleClassification extends App {

  abstract class Classifier(predefinedClassifications: Double*) {
    var index = 0

    def classify(data: Int) = {
      val c = predefinedClassifications(index)
      if (predefinedClassifications.size - 1 == index) index = 0
      else index += 1
      c
    }
  }

  //just some random classifications that we pretend come from some testdata
  object low extends Classifier(.05, .15, .11, .21, .33, .1, .16)

  object lowMedium extends Classifier(.01, .1, .11, .13, .15, .25, .33, .5, .6, .55, .66, .5, .2, .56, .7, .12, .41, .45, .52)

  object medium extends Classifier(.4, .5, .6, .65, .7, .6, .5, .5, .46, .7, .51)

  object high extends Classifier(.85, .9, .77, .95, .99, .92, .78, .87, .82, .85, .8, .83, .97)

  object all extends Classifier(.01, .07, .14, .21, .28, .35, .42, .49, .5, .56, .63, .7, .77, .84, .91, .98, .99)

  val classifiers = List(low, lowMedium, medium, high, all)
  val testData = (0 until 100).toArray
  val predictions: Array[Double] = testData.flatMap(t => classifiers.map(_.classify(t)))
  val predicted: DenseMatrix[Double] = new DenseMatrix(classifiers.size, testData.size, predictions).t
  println(predicted)
  var i = 0
  val labeled: DenseVector[Double] = predicted(*, ::).map { row =>
    0d
  }

  import com.kodeworks.doffapp.nlp._

  val weights: DenseVector[Double] = weighLabeledPredictions(predicted, labeled)
  println("weights: " + weights)

  val weightedMeans: DenseVector[Double] = predicted(*, ::).map { row =>
    predictionsWeightedMean(row, weights)
  }
  println("weightedMeans: " + weightedMeans)
}
