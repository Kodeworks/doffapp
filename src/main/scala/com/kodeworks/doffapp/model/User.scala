package com.kodeworks.doffapp.model

import java.util.UUID

case class User(
                 id: Option[Long] = None,
                 name: String = UUID.randomUUID().toString
               )

object User {

  object Json {
    import argonaut.CodecJson.derive
    import argonaut._
    implicit val UserCodec = derive[User]
  }

}
