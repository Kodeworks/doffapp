package com.kodeworks.doffapp.nlp

import com.kodeworks.doffapp.nlp.CompoundSplitter._
import com.kodeworks.doffapp.nlp.wordbank.Wordbank

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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
//TODO words with triple consonants that have been subtracted to double consonants, i.e 'akuttilfellet'
//TODO allowed word classes
//TODO keep trying to split while any of the split words are greater than max length
class CompoundSplitter(
                        wordbank: Wordbank,
                        minLength: Int = minLengthDefault,
                        maxLength: Int = maxLengthDefault,
                        wordclassRestrictions: Map[String, String] = wordclassRestrictionsDefault,
                        binders1: Set[Char] = binders1Default,
                        binders2: Set[String] = binders2Default
                      ) {

  import wordbank._

  val wordsFullToBase: Map[String, String] = wordbankWords.filter(_.full.length >= minLength).map(w => w.full -> w.base).toMap
  val words = wordsFullToBase.keySet

  def splitN(compound: String): List[List[String]] = {
    //heedMaxLength: we want to do at least one split attempt
    val memo = mutable.Map[String, List[ListBuffer[String]]]()
    def splitNRec(compound0: String, heedMaxLength: Boolean = true): List[ListBuffer[String]] = {
      if (heedMaxLength && compound0.length < maxLength) Nil //List(List(compound0))
      else {
        val splitted = split(compound0)
        //TODO log.trace
        //println(s"first split $compound0 => $splitted")
        splitted.flatMap { split0 =>
          val buf = ListBuffer[ListBuffer[String]]()
          split0.foreach { split1 =>
            val subsplits = memo.getOrElseUpdate(split1, splitNRec(split1))
            if (subsplits.isEmpty)
              if (buf.isEmpty) buf += ListBuffer(split1)
              else buf.foreach(_ += split1)
            else if (1 == subsplits.size)
              if (buf.isEmpty) buf += ListBuffer(subsplits(0): _*)
              else buf.foreach(_ ++= subsplits(0))
            else {
              val size = buf.size
              buf.sizeHint(buf.size * subsplits.size)
              var i = 0
              while (i < size) {
                val bufi = buf(i)
                var j = 1
                while (j < subsplits.size) {
                  val buf0 = ListBuffer[String]((bufi ++ subsplits(j)): _*)
                  buf += buf0
                  j += 1
                }
                bufi ++= subsplits(0)
                i += 1
              }
            }
          }
          buf
        }
      }
    }
    splitNRec(compound, heedMaxLength = false).distinct.map(_.toList)
  }

  def split(compound: String): List[List[String]] = {
    if (2 * compound.length < minLength) return Nil
    val splits = ListBuffer[List[String]]()
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
          splits += List(split0, split1)
        }
        if (binders1.contains(compound.charAt(split0.length))) {
          val split2 = compound.substring(split0.length + 1)
          if (words.contains(split2) && notBase) splits += List(split0, split2)
        }
        if (binders2.contains(compound.substring(split0.length, split0.length + 2))) {
          val split3 = compound.substring(split0.length + 2)
          if (words.contains(split3) && notBase) splits += List(split0, split3)
        }
      }
      i += 1
    }
    //TODO eval if this is a better api
    //    if (splits.isEmpty) splits += List(compound)
    splits.toList
  }

  private def splitBuf(compound: String): ListBuffer[ListBuffer[String]] = {
    if (2 * compound.length < minLength) return ListBuffer.empty
    val splits = ListBuffer[ListBuffer[String]]()
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
          splits += ListBuffer(split0, split1)
        }
        if (binders1.contains(compound.charAt(split0.length))) {
          val split2 = compound.substring(split0.length + 1)
          if (words.contains(split2) && notBase) splits += ListBuffer(split0, split2)
        }
        if (binders2.contains(compound.substring(split0.length, split0.length + 2))) {
          val split3 = compound.substring(split0.length + 2)
          if (words.contains(split3) && notBase) splits += ListBuffer(split0, split3)
        }
      }
      i += 1
    }
    splits
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
  val minLengthDefault = 5
  val maxLengthDefault = 7
  val wordclassRestrictionsDefault = Map(
    "subst" -> "subst",
    "subst" -> "verb", //?
    "verb" -> "verb")
}