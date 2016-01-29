package com.kodeworks.doffapp.model

import java.util.UUID

case class User(
                 id: Option[Long] = None,
                 name: String = UUID.randomUUID().toString
               )

object User {

  object Json {
    import argonaut._
    import Argonaut._
    import CodecJson.derive

    implicit val UserCodec = derive[User]
  }

}
