package com.kodeworks.doffapp.model

case class Classify(
                     user: String,
                     tender: String,
                     label: String = "1",
                     time: Long = System.currentTimeMillis,
                     id: Option[Long] = None) {
  override def equals(other: Any): Boolean = other match {
    case that: Classify =>
      (that canEqual this) &&
        user == that.user &&
        tender == that.tender &&
        time == that.time
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(user, tender, time)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object Classify {

  object Json {

    import argonaut.CodecJson.derive
    import argonaut._

    implicit val ClassifyCodec = derive[Classify]
  }

}
