package com.kodeworks.doffapp.model

import java.util.UUID

case class User(
                 name: String = UUID.randomUUID().toString,
                 id: Option[Long] = None
               )

object User {

  object Json {

    import argonaut.CodecJson.derive
    import argonaut._

    implicit val UserCodec = derive[User]
  }

}
