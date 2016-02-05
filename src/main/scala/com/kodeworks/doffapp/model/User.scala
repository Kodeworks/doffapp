package com.kodeworks.doffapp.model

import java.util.UUID

case class User(
                 name: String = UUID.randomUUID().toString,
                 id: Option[Long] = None
               )

object User {

}
