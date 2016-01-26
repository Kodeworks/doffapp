package com.kodeworks.doffapp

import com.kodeworks.doffapp.model.{User, Classify, Tender}

package object message {

  case object InitSuccess
  case object InitFailure
  case object InitTimeout

  case class SaveTenders(tenders: Seq[Tender])
  case class SaveClassifys(classifys: Seq[Classify])
  case class SaveUsers(users: Seq[User])
}
