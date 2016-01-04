package com.kodeworks.doffapp

import java.time.Instant

import akka.http.scaladsl.model.Uri.Path
import com.kodeworks.doffapp.model.{InputOutput, Tender}
import com.kodeworks.doffapp.nlp.Brain
import org.junit.{Assert, Test}
import argonaut._
import Argonaut._

import scala.util.Random

class BrainTest {
  val t0 = Tender(
    "NH3-kuldeanlegg",
    Path("/Notice/Details/2015-132151"),
    "no",
    "S\u00f8r-Tr\u00f8ndelag fylkeskommune",
    Some("https://kgv.doffin.no/ctm/Supplier/CompanyInformation/Index/2648"),
    "2015-132151",
    "Kunngj\u00f8ring av konkurranse",
    Instant.parse("2015-11-25T00:00:00Z"),
    Some(Instant.parse("2016-01-15T00:00:00Z")),
    Some("Trondheim"),
    Some("S\u00f8r-Tr\u00f8ndelag"),
    Some("https://kgv.doffin.no/ctm/Supplier/Documents/Folder/137932"))
  val t1 = Tender(
    "Biff",
    Path("/slafs"),
    "no",
    "S\u00f8r-Tr\u00f8ndelag fylkeskommune",
    Some("gruff"),
    "asdf",
    "giff",
    Instant.parse("2014-11-25T00:00:00Z"),
    Some(Instant.parse("2017-01-15T00:00:00Z")),
    Some("Trondheim"),
    Some("S\u00f8r-Tr\u00f8ndelag"),
    Some("hepp"))
  val t2 = Tender(
    "Nuff",
    Path("gibba"),
    "no",
    "S\u00f8r-Tr\u00f8ndelag fylkeskommune",
    Some("nadfa"),
    "gremlin",
    "gegg",
    Instant.parse("2005-11-25T00:00:00Z"),
    Some(Instant.parse("2006-01-15T00:00:00Z")),
    Some("Trondheim"),
    Some("S\u00f8r-Tr\u00f8ndelag"),
    Some("nibb"))
  val t3 = Tender(
    "gurba",
    Path("garg"),
    "no",
    "duff",
    Some("hubb"),
    "gnagg",
    "hubba",
    Instant.parse("2012-11-25T00:00:00Z"),
    Some(Instant.parse("2011-01-15T00:00:00Z")),
    Some("Oslo"),
    Some("Oslo"),
    Some("gakka"))

  //  @Test
  //test fails - brain.js does not like non-numeric data. Thus we need some kind of dictionary.
  def test {
    val yesTenders = List(t0, t1)
    val noTenders = List(t3)
    val input = t2
    import InputOutput._
    //    val tenders = yesTenders.map(_ -> true) ++ noTenders.map(_ -> false)
    //    Brain.train(tenders)
    //    Brain.run(input)
  }

//  @Test
  //test fails. seems difficult to use naive bayes classification to determine geographical points of interest. Thus maybe better to just use place names.
  def testLatLng {
    val min = 0
    val max = 100
    val numValues = 10000

    val minInterresting = 55
    val maxInterresting = 57

    def coord = min + math.random * (max - min)
    val latLngs = (0 to numValues map (_ => coord -> coord)).toList
    val inputOutputs: List[(List[Double], List[Int])] = latLngs.map { ll =>
      if (minInterresting < ll._1 && ll._1 < maxInterresting
        && minInterresting < ll._2 && ll._2 < maxInterresting) {
        println(ll._1 + ", " + ll._2)
        List(ll._1, ll._2) -> List(1)
      }
      else List(ll._1, ll._2) -> List(0)
    }
    println(inputOutputs.count(p => p._2.forall(_ == 1)))
    import argonaut._
    import Argonaut._
    import InputOutput._
    implicit val ioc = inputOutputCodec[List[Double], List[Int]]
    println(inputOutputs.map(io => InputOutput(io._1, io._2)).asJson.nospaces)
    Brain.train(inputOutputs)
    val res = Brain.run(List(56d, 56d))
    println(res)
    Assert.assertTrue(res(0) > .75)
  }
}