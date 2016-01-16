package com.kodeworks.doffapp.model

import java.time.Instant

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path

case class Tender(
                   name: String,
                   internalUrl: String,
                   flag: String,
                   publishedBy: String,
                   publishedByUrl: Option[String],
                   doffinReference: String,
                   announcementType: String,
                   announcementDate: Long,
                   tenderDeadline: Option[Long],
                   county: Option[String],
                   municipality: Option[String],
                   externalUrl: Option[String],
                   id: Option[Long] = None
                 )

object Tender {

  import argonaut._
  import Argonaut._
  import CodecJson.derive

  implicit val pathCodec = CodecJson((p: Path) => jString(p.toString), _.as[String].map(Path(_)))
  implicit val uriCodec = CodecJson((u: Uri) => jString(u.toString), _.as[String].map(Uri(_)))
  implicit val instantCodec = CodecJson((i: Instant) => jNumber(i.toEpochMilli), _.as[Long].map(Instant.ofEpochMilli(_)))
  implicit val tenderCodec = derive[Tender]
}