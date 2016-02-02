import breeze.linalg.Counter
import nak.classify.NaiveBayes
import nak.data.Example

object Testy extends App {
  val trainer = new NaiveBayes.Trainer[Boolean, String]()
  val trainingData = Array(
    Example(true, Counter.count("omsorg bolig i ål sentrum".split(" "):_*).mapValues(_.toDouble)),
    Example(true, Counter.count("evaluering av tjeneste tilbud til person med behov for lindre omsorg".split(" "):_*).mapValues(_.toDouble)),
    Example(true, Counter.count("vikartjeneste til omsorg sektor i kristiansund kommune".split(" "):_*).mapValues(_.toDouble)),
    Example(true, Counter.count("evaluering av forsøk ordning med statlig finansiering av kommunal omsorg tjeneste sio".split(" "):_*).mapValues(_.toDouble)),
    Example(false, Counter.count("kjøp av tolkefomidlingstjenester til nittedal kommune".split(" "):_*).mapValues(_.toDouble)),
    Example(false, Counter.count("nettportal orkdal og meldal kommune".split(" "):_*).mapValues(_.toDouble)),
    Example(false, Counter.count("trykksak og trykkeri tjeneste".split(" "):_*).mapValues(_.toDouble))
  )
  val testData = Array(
    Example(true, Counter.count("anskaffelse av heldøgns bo og omsorg tjeneste for en bruker".split(" "):_*).mapValues(_.toDouble))
  )

  val train: NaiveBayes[Boolean, String] = trainer.train(trainingData)
  val r = train.classify(testData(0).features)
  println(r)
  val f = train.scores(testData(0).features)
  println(f)
}
