package com.kodeworks.doffapp.nlp

import com.kodeworks.doffapp.nlp.CompoundSplitter._
import com.kodeworks.doffapp.nlp.wordbank.Word

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
                        words: List[Word],
                        wordclassRestrictions: Map[String, String] = wordclassRestrictionsDefault,
                        binders1: List[String] = binders1Default,
                        binders2: List[String] = binders2Default
                      ) {
  def splitCompound(compound: String) = {
    val (zeros: List[String], nonzeros: Map[Int, List[String]]) = words
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
    zeros.flatMap { zero =>
      val hasPlusoneBinder = zero.length < compound.length && binders1.contains(compound.substring(zero.length, zero.length + 1))
      val hasPlustwoBinder = zero.length + 1 < compound.length && binders2.contains(compound.substring(zero.length, zero.length + 2))
      def getNonzero(start: Int = 0) =
        nonzeros.getOrElse(zero.length + start, Nil).filter { x =>
          x.length == compound.length - zero.length - start
        }
      val pluszeros = getNonzero()
      val plusones = if (hasPlusoneBinder) getNonzero(1) else Nil
      val plustwos = if (hasPlustwoBinder) getNonzero(2) else Nil
      (pluszeros ++ plusones ++ plustwos).map(zero -> _)
    }
  }
}

object CompoundSplitter {
  val binders1Default = List("e", "s", "a")
  val binders2Default = List("er")
  val wordclassRestrictionsDefault = Map(
    "subst" -> "subst",
    "subst" -> "verb",
    "verb" -> "verb")
}