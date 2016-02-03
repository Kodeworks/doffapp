import com.kodeworks.doffapp.nlp.BowFeaturizer
import nak.NakContext
import nak.classify.NaiveBayes
import nak.core.LiblinearClassifier
import nak.data.{ExactFeatureMap, Example, FeatureObservation, TfidfBatchFeaturizer}
import nak.liblinear.LiblinearConfig

object Testy extends App {
  val trainer = new NaiveBayes.Trainer[String, String]()
  val trainingData = Seq(
    (1, "omsorg bolig i ål sentrum"),
    (1, "evaluering av tjeneste tilbud til person med behov for lindre omsorg"),
    (1, "vikartjeneste til omsorg sektor i kristiansund kommune"),
    (1, "evaluering av forsøk ordning med statlig finansiering av kommunal omsorg tjeneste sio"),
    (1, "tjeneste design og bistand med utvikling av modell innenfor to årige utvikling program i kommunal omsorg sektor"),
    (0, "kjøp av tolkefomidlingstjenester til nittedal kommune"),
    (0, "nettportal orkdal og meldal kommune"),
    (0, "trykksak og trykkeri tjeneste"),
    (0, "kopi av juridisk konsulent tjeneste i angola"),
    (0, "anskaffelse av vedlikehold tjeneste drift bruker støtte og vedlikeholdstjeneser av sius nettsted"),
    (0, "lunde barneskule ny klasseromsfløy"),
    (0, "kns i kurs og konferanse utenfor kristiansand sentrum"),
    (0, "konvergere hyperkonvergert løsning"),
    (0, "djupvik kryssingsspor byggeledelse"),
    (0, "rammeavtale vannbehandling service og kjøp av deler"),
    (0, "høyhastighetskamera"),
    (0, "vekter tjeneste til bemanning av resepsjon på oslo city"),
    (0, "personlig prøvetaking pump for det lave luftgjennomstrømningshastighetsområdet"),
    (0, "anskaffelse av automatisere traverskran"),
    (0, "uteområde veslefrikk barnehage"),
    (0, "rammeavtale for kjøp av kontor rekvisita"),
    (0, "tjeneste design og bistand med utvikling av modell innenfor to årige utvikling program i kommunal omsorg sektor"),
    (0, "anskaffelse av automatisere traverskran"),
    (0, "uteområde veslefrikk barnehage"),
    (0, "rammeavtale for kjøp av kontor rekvisita"),
    (0, "ma e rammeavtale for kjøp av drosje tjeneste for nrk i bergen og trondheim")
  )
  val testData = Seq(
    (1, "anskaffelse av heldøgns bo og omsorg tjeneste for en bruker"), //
    (1, "rauland omsorg senter ombygging ventilasjon anlegg"), //correct on all
    (0, "avklaring med jobbsøk og praksis") //error on all
  )

  //Naive Bayes
  //  def toNbData(data: Seq[(Int, String)]): Seq[Example[String, Counter[String, Double]]] =
  //    data.map(t => Example(t._1.toString, Counter.count(t._2.split(" "): _*).mapValues(_.toDouble)))
  //  val nbExamples = toNbData(trainingData)
  //  val nbTest = toNbData(testData)
  //  val nbClassifier: NaiveBayes[String, String] = trainer.train(nbExamples)
  //  nbTest.foreach { nbt =>
  //    val f0 = nbClassifier.scores(nbt.features).toMap.toList.sortBy(_._1)
  //    println(f0.mkString(", "))
  //  }
  //  println

  //Bag of Words
  def toBowData(data: Seq[(Int, String)]) =
    data.map(t => Example(t._1.toString, t._2))

  val bowExamples = toBowData(trainingData)
  val bowTest = toBowData(testData)

  val bowFeaturizer = new BowFeaturizer()
  val liblinearConfig = LiblinearConfig(cost = 5d, eps = .1)
  //TODO play with liblinearConfig
  val bowClassifier = NakContext.trainClassifier(liblinearConfig, bowFeaturizer, bowExamples)
  val lmap: Map[String, Int] = bowClassifier.asInstanceOf[LiblinearClassifier].lmap
  bowTest.foreach { bt =>
    val f1 = bowClassifier.evalRaw(bt.features)
      .zipWithIndex.map { case (r, i) => bowClassifier.labelOfIndex(i) -> r }.toMap.toList.sortBy(_._1)
    println(f1.mkString(", "))
  }
  println

  //TfIdf
  val tfidfBatchFeaturizer = new TfidfBatchFeaturizer[String](0)
  //test without 0, that is, 2 instead
  val tfidfExamples: Seq[Example[Int, Seq[FeatureObservation[Int]]]] =
    tfidfBatchFeaturizer(bowExamples).map(_.map(_.map(_.map(bowClassifier.indexOfFeature(_).get)).sortBy(_.feature)).relabel(bowClassifier.indexOfLabel(_)))
  val tfidfTest: Seq[Example[String, Seq[FeatureObservation[String]]]] = bowTest.map(_.map(bowFeaturizer(_)))
  val tfidfClassifier = NakContext.trainClassifier(liblinearConfig, tfidfExamples,
    lmap,
    bowClassifier.asInstanceOf[LiblinearClassifier].fmap.asInstanceOf[ExactFeatureMap].fmap)
  //  val leastSignificantWords: List[(String, Double)] = tfidfFeaturized.flatMap(_.features).groupBy(_.feature).mapValues(_.minBy(_.magnitude).magnitude).toList.sortBy(lm => -lm._2)
  //  val stopwords: Set[String] = leastSignificantWords.take(30).map(_._1).toSet
  tfidfTest.foreach { tt =>
    val r2 = tfidfClassifier.evalUnindexed(tt.features)
      .zipWithIndex.map { case (r, i) => bowClassifier.labelOfIndex(i) -> r }.toMap.toList.sortBy(_._1)
    println(r2.mkString(", "))
  }
}
