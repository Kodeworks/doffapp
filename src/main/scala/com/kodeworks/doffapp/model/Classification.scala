package com.kodeworks.doffapp.model

// The result of a classification of a tender based on all tenders a user has classified.
// Term-frequency/inverse-document-frequency.
// Bag-of-words.
case class Classification(
                           tender: String,
                           user: String,
                           tfidf: Map[String, Double],
                           bow: Map[String, Double]
                         )

object Classification {

  object Json {

    import argonaut.CodecJson.derive
    import argonaut._

    implicit val UserCodec = derive[Classification]
  }

}
