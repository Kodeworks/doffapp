package com.kodeworks.doffapp.model

import java.time.Instant

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path

case class Tender(
                   name: String,
                   internalUrl: Path,
                   flag: String,
                   publishedBy: String,
                   publishedByUrl: Option[Uri],
                   doffinReference: String,
                   announcementType: String,
                   announcementDate: Instant,
                   tenderDeadline: Option[Instant],
                   county: Option[String],
                   municipality: Option[String],
                   externalUrl: Option[Uri]
                 )

object Tender {
  import argonaut._
  import Argonaut._
  import CodecJson.derive

  //TODO not entirely happy with these - should not have an inner field
  implicit val pathCodec: CodecJson[Path] = codec1(Path(_: String), (_: Path).toString)("path")
  implicit val uriCodec: CodecJson[Uri] = codec1(Uri(_: String), (_: Uri).toString)("uri")
  implicit val instantCodec: CodecJson[Instant] = codec1(Instant.ofEpochMilli, (_: Instant).toEpochMilli)("instant")
  implicit val tenderCodec = derive[Tender]
}