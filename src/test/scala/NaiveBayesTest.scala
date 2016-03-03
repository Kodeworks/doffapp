import breeze.linalg._
import breeze.math._
import breeze.numerics._
import nak.classify.NaiveBayes
import nak.data.Example

object NaiveBayesTest extends App {
  //  def unnormalizedLogProb2prob(ulp: Double) =
  //    math.exp(ulp - softmax(ulp))

  val train = List(
    "1" -> "alpha bravo charlie delta echo foxtrot",
    "1" -> "bravo delta foxtrot golf",
    "1" -> "alpha bravo echo hotel indigo juliet",
    "1" -> "charlie echo foxtrot kilo lima mike",
    "0" -> "alpha kilo lima mike november juliet indigo",
    "0" -> "golf hotel indigo juliet kilo mike november",
    "0" -> "bravo charlie hotel indigo kilo november",
    "0" -> "echo foxtrot indigo hotel golf juliet"
  ).map(t => Example(t._1, Counter.count(t._2.split(" "): _*).mapValues(_.toDouble)))

  val nb = new NaiveBayes(train)
  val scores  = nb.normScores(Counter.count("alpha bravo charlie delta kilo".split(" "): _*).mapValues(_.toDouble))

  println(scores)
}
