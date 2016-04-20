package com.kodeworks.doffapp

import shapeless._

import scala.collection.mutable
import scala.reflect.runtime.universe._

trait IdGen {
  implicit val ltag: TypeTag[L] forSome {type L <: HList}
  private val idGens = mutable.Map(ltag.toString.split("""::\[""").toList.tail.map(_.split(""",shapeless""")(0) -> 0L): _*)

  //getter
  def id[T: TypeTag] = {
    val t = typeOf[T].toString
    val id = idGens(t)
    idGens(t) = id + 1L
    id
  }

  //setter
  def id[T: TypeTag](id: Long) {
    val t = typeOf[T].toString
    idGens(t) = id
  }
}