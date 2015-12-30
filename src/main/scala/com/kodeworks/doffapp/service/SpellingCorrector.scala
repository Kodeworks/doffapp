package com.kodeworks.doffapp.service

import scala.io.Codec

class SpellingCorrector(dict: Map[String, Int])(implicit codec: Codec = Codec.ISO8859) {
  val alphabet = ('a' to 'z' toArray) ++ Array('\u00E6', '\u00F8', '\u00E5')

  def edits(s: Seq[(String, String)]): Seq[String] =
    (for ((a, b) <- s; if b.length > 0) yield a + b.substring(1)) ++
    (for ((a, b) <- s; if b.length > 1) yield a + b(1) + b(0) + b.substring(2)) ++
    (for ((a, b) <- s; c <- alphabet if b.length > 0) yield a + c + b.substring(1)) ++
    (for ((a, b) <- s; c <- alphabet) yield a + c + b)

  def edits1(word: String): Seq[String] = edits(for (i <- 0 to word.length) yield (word take i, word drop i))

  def edits2(word: String): Seq[String] = for (e1 <- edits1(word); e2 <- edits1(e1)) yield e2

  def known(words: Seq[String]): Seq[String] = for (w <- words; found <- dict.get(w)) yield w

  def or[T](candidates: Seq[T], other: => Seq[T]): Seq[T] = if (candidates.isEmpty) other else candidates

  def candidates(word: String): Seq[String] = or(known(List(word)), or(known(edits1(word)), known(edits2(word))))

  def correct(word: String) = ((-1, word) /: candidates(word)) (
    (max, word) => if (dict(word) > max._1) (dict(word), word) else max)._2
}
