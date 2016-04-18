package com.kodeworks.doffapp

import shapeless._

import scala.collection.mutable
import scala.reflect.runtime.universe._

trait IdGen[L <: HList] {
  implicit val ltag: TypeTag[L]
  private val idGens = mutable.Map(typeTag[L].toString.split("""::\[""").toList.tail.map(_.split(""",shapeless""")(0) -> 0L): _*)

  def id[T: TypeTag] = {
    val t = typeOf[T].toString
    val id = idGens(t)
    idGens(t) = id + 1L
    id
  }
}
