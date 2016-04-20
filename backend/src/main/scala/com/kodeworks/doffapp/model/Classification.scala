package com.kodeworks.doffapp.model

// The result of a classification of a tender based on all tenders a user has classified.
// Term-frequency/inverse-document-frequency.
// Bag-of-words.
// Naive Bayes
case class Classification(
                           tender: String,
                           user: String,
                           tfidf: Double,
                           bow: Double,
                           nb: Double,
                           weighted: Double,
                           mean: Double
                         )

object Classification {
}
