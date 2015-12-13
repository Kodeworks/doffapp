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