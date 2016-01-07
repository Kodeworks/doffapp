package com.kodeworks.doffapp.nlp

import com.kodeworks.doffapp.nlp.CompoundSplitter._
import com.kodeworks.doffapp.nlp.wordbank.Wordbank

import scala.collection.mutable.ListBuffer
import scala.collection.{GenSeq, GenMap}

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
//TODO prefix instead of whole list
//TODO words with triple consonants that have been subtracted to double consonants, i.e 'akuttilfellet'
class CompoundSplitter(
                        wordbank: Wordbank,
                        minLength: Int = minLengthDefault,
                        wordclassRestrictions: Map[String, String] = wordclassRestrictionsDefault,
                        binders1: Set[Char] = binders1Default,
                        binders2: Set[String] = binders2Default
                      ) {

  import wordbank._

  val wordsFullToBase: Map[String, String] = wordbankWords.filter(_.full.length >= minLength).map(w => w.full -> w.base).toMap
  val words = wordsFullToBase.keySet

  def splitCompound(compound: String): List[(String, String)] = {
    if (2 * compound.length < minLength) return Nil
    val splits = ListBuffer[(String, String)]()
    var i = 0
    while (minLength * 2 + i <= compound.length) {
      val split0 = compound.substring(0, minLength + i)
      if (words.contains(split0)) {
        val split1 = compound.substring(split0.length)
        var isBase = false
        def notBase =
          if (isBase) false
          else {
            isBase = wordsFullToBase.get(compound).contains(split0)
            !isBase
          }
        if (words.contains(split1) && notBase) {
          splits += split0 -> split1
        }
        if (binders1.contains(compound.charAt(split0.length))) {
          val split2 = compound.substring(split0.length + 1)
          if (words.contains(split2) && notBase) splits += split0 -> split2
        }
        if (binders2.contains(compound.substring(split0.length, split0.length + 2))) {
          val split3 = compound.substring(split0.length + 2)
          if (words.contains(split3) && notBase) splits += split0 -> split3
        }
      }
      i += 1
    }
    splits.toList
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
  val binders1Default = Set('e', 's', 'a')
  val binders2Default = Set("er")
  val minLengthDefault = 3
  val wordclassRestrictionsDefault = Map(
    "subst" -> "subst",
    "subst" -> "verb",
    "verb" -> "verb")
}