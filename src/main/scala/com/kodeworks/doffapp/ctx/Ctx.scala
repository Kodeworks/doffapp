package com.kodeworks.doffapp.ctx

import com.kodeworks.doffapp.nlp.MostUsedWords
import com.kodeworks.doffapp.nlp.wordbank.Wordbank

trait Ctx
  extends Cfg
    with Prop
    with Files
    with Wordbank
    with MostUsedWords
    with Nlp
    with Db
    with Actors
    with Http

object Ctx extends Ctx
