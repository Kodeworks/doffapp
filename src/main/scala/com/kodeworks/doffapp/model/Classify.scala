package com.kodeworks.doffapp.model

case class Classify(
                     userName: String,
                     tenderDoffinReference: String,
                     time: Long,
                     id: Option[Long] = None) {
  override def equals(other: Any): Boolean = other match {
    case that: Classify =>
      (that canEqual this) &&
        userName == that.userName &&
        tenderDoffinReference == that.tenderDoffinReference &&
        time == that.time
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(userName, tenderDoffinReference, time)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
