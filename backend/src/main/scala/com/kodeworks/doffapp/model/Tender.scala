package com.kodeworks.doffapp.model

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
}