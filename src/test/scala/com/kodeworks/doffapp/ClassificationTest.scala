package com.kodeworks.doffapp

import com.kodeworks.doffapp.ctx.NlpImpl
import com.kodeworks.doffapp.nlp.ClassifierFactory

object ClassificationTest extends App {

  val trainingData: Seq[(String, String)] = Seq(
    ("1", "omsorg bolig i ål sentrum"),
    ("1", "evaluering av tjeneste tilbud til person med behov for lindre omsorg"),
    ("1", "vikartjeneste til omsorg sektor i kristiansund kommune"),
    ("1", "evaluering av forsøk ordning med statlig finansiering av kommunal omsorg tjeneste sio"),
    ("1", "tjeneste design og bistand med utvikling av modell innenfor to årige utvikling program i kommunal omsorg sektor"),
    ("0", "kjøp av tolkefomidlingstjenester til nittedal kommune"),
    ("0", "nettportal orkdal og meldal kommune"),
    ("0", "trykksak og trykkeri tjeneste"),
    ("0", "kopi av juridisk konsulent tjeneste i angola"),
    ("0", "anskaffelse av vedlikehold tjeneste drift bruker støtte og vedlikeholdstjeneser av sius nettsted"),
    ("0", "lunde barneskule ny klasseromsfløy"),
    ("0", "kns i kurs og konferanse utenfor kristiansand sentrum"),
    ("0", "konvergere hyperkonvergert løsning"),
    ("0", "djupvik kryssingsspor byggeledelse"),
    ("0", "rammeavtale vannbehandling service og kjøp av deler"),
    ("0", "høyhastighetskamera"),
    ("0", "vekter tjeneste til bemanning av resepsjon på oslo city"),
    ("0", "personlig prøvetaking pump for det lave luftgjennomstrømningshastighetsområdet"),
    ("0", "anskaffelse av automatisere traverskran"),
    ("0", "uteområde veslefrikk barnehage"),
    ("0", "rammeavtale for kjøp av kontor rekvisita"),
    ("0", "tjeneste design og bistand med utvikling av modell innenfor to årige utvikling program i kommunal omsorg sektor"),
    ("0", "anskaffelse av automatisere traverskran"),
    ("0", "uteområde veslefrikk barnehage"),
    ("0", "rammeavtale for kjøp av kontor rekvisita"),
    ("0", "ma e rammeavtale for kjøp av drosje tjeneste for nrk i bergen og trondheim")
  )
  val testData: Seq[(String, String)] = Seq(
    ("1", "anskaffelse av heldøgns bo og omsorg tjeneste for en bruker"), //
    ("1", "rauland omsorg senter ombygging ventilasjon anlegg"), //correct on all
    ("0", "avklaring med jobbsøk og praksis") //error on all
  )

  //Naive Bayes
  //  val trainer = new NaiveBayes.Trainer[String, String]()
  //  def toNbData(data: Seq[(Int, String)]): Seq[Example[String, Counter[String, Double]]] =
  //    data.map(t => Example(t._"1".toString, Counter.count(t._2.split(" "): _*).mapValues(_.toDouble)))
  //  val nbExamples = toNbData(trainingData)
  //  val nbTest = toNbData(testData)
  //  val nbClassifier: NaiveBayes[String, String] = trainer.train(nbExamples)
  //  nbTest.foreach { nbt =>
  //    val f"0" = nbClassifier.scores(nbt.features).toMap.toList.sortBy(_._"1")
  //    println(f"0".mkString(", "))
  //  }
  //  println

  trait Ctx extends TestCtx {
    override val mostUsedWordsTop64: Set[String] = Set()
  }

  object ctx extends Ctx with NlpImpl

  val cf = new ClassifierFactory(ctx, trainingData.map(_._2))
  val c = cf.classifier(trainingData)
  //  val leastSignificantWords: List[(String, Double)] = tfidfFeaturized.flatMap(_.features).groupBy(_.feature).mapValues(_.minBy(_.magnitude).magnitude).toList.sortBy(lm => -lm._2)
  //  val stopwords: Set[String] = leastSignificantWords.take(3"0").map(_._"1").toSet
  testData.foreach { td =>
    println(td._1 + " bow   " + c.bow(td._2))
    println(td._1 + " tfidf " + c.tfidf(td._2))
  }
}
