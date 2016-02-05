import com.kodeworks.doffapp.model.{Classification, Tender}

import argonaut._, Argonaut._, Shapeless._

object Testy extends App {
  val tender = Tender("name", "internalUrl", "flag", "publishedBy", Some("publishedByUrl"), "doffinReference", "announcementType", 1L, Some(1L), Some("county"), Some("muncipality"), Some("externalurl"))
  val classification = Classification("doffinReference", "abc123", Map("0" -> .123, "1" -> .877), Map("0" -> .223, "1" -> .777))
  val tup = (tender, classification)
  println(tup.asJson)
  val seq = Seq(tender, tender)
  println(seq.asJson)
}
