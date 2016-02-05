package com.kodeworks.doffapp

import java.time.Instant

import akka.http.scaladsl.model.Uri.Path
import argonaut._, Argonaut._, Shapeless._
import com.kodeworks.doffapp.model.Tender
import org.junit.{Assert, Test}

class TenderTest {

  @Test
  def testTenderCodec {
    import argonaut.Argonaut._

    val tender = Tender(
      "NH3-kuldeanlegg",
      "/Notice/Details/2015-132151",
      "no",
      "S\u00f8r-Tr\u00f8ndelag fylkeskommune",
      Some("https://kgv.doffin.no/ctm/Supplier/CompanyInformation/Index/2648"),
      "2015-132151",
      "Kunngj\u00f8ring av konkurranse",
      Instant.parse("2015-11-25T00:00:00Z").toEpochMilli,
      Some(Instant.parse("2016-01-15T00:00:00Z").toEpochMilli),
      Some("Trondheim"),
      Some("S\u00f8r-Tr\u00f8ndelag"),
      Some("https://kgv.doffin.no/ctm/Supplier/Documents/Folder/137932"))
    val json = tender.asJson
    Assert.assertTrue(json.hasField("internalUrl"))
  }
}
