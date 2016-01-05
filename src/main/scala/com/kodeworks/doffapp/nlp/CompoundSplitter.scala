package com.kodeworks.doffapp.nlp

import com.kodeworks.doffapp.nlp.CompoundSplitter._
import com.kodeworks.doffapp.nlp.wordbank.Wordbank

/**
  * Compound splitting
for each word in dict, collect those that are contained in compound, start and end index
split list in those with start index 0 and start index non-0 (zeros, nonzeros)
group nonzeros by start index
for each in zeros, get each in nonzeros with start index equal to
- zeros end index - 1 (in case of overlapping chars, like "konsulentjenester" where 't' overlaps
- zeros end index (no overlap or inbetween chars, like "bingokveld")
- zeros end index + 1 (in case of in-between chars like 's' between the two words, like "betalingsbetingelser")
browse..
  */
class CompoundSplitter(
                        wordbank: Wordbank,
                        wordclassRestrictions: Map[String, String] = wordclassRestrictionsDefault,
                        binders1: List[String] = binders1Default,
                        binders2: List[String] = binders2Default
                      ) {

  import wordbank._

  def splitCompound(compound: String) = {
    val (firsts: List[String], seconds: Map[Int, List[String]]) = wordbankWords
      .map(_.full).distinct
      .collect { case word0 => word0 -> compound.indexOf(word0) }
      .filter(ws =>
        ws._2 != -1 //ignore not found
          && ws._1.length < compound.length - 1 //ignore full or one-from-full matches
          && 1 < ws._1.length) // ignore one-length matches
      .partition {
      case (w, 0) => true
      case _ => false
    } match {
      case (firsts, seconds) => firsts.map(_._1) -> seconds.groupBy(_._2).map(g => g._1 -> g._2.map(_._1))
    }
    firsts.flatMap { first =>
      val hasPlusoneBinder = first.length < compound.length && binders1.contains(compound.substring(first.length, first.length + 1))
      val hasPlustwoBinder = first.length + 1 < compound.length && binders2.contains(compound.substring(first.length, first.length + 2))
      def getNonzero(start: Int = 0) =
        seconds.getOrElse(first.length + start, Nil).filter { second =>
          second.length == compound.length - first.length - start && //first, binder and second equals compound length
            !altEndings(first).contains(compound.substring(first.length, first.length + start) + second) //second is not an alternative ending of first
        }
      val pluszeros = getNonzero()
      val plusones = if (hasPlusoneBinder) getNonzero(1) else Nil
      val plustwos = if (hasPlustwoBinder) getNonzero(2) else Nil
      (pluszeros ++ plusones ++ plustwos).map(first -> _)
    }
  }

  def alts(w: String) = wordbankWordsBaseToFull(wordbankWordsFullToBase(w))

  def altEndings(w: String) =
    alts(w)
      .map(ww => ww -> ww.indexOf(w))
      .filter(ww =>
        ww._2 != -1 &&
          w.length < ww._1.length)
      .map(_._1.substring(w.length))
}

object CompoundSplitter {
  val binders1Default = List("e", "s", "a")
  val binders2Default = List("er")
  val wordclassRestrictionsDefault = Map(
    "subst" -> "subst",
    "subst" -> "verb",
    "verb" -> "verb")
}