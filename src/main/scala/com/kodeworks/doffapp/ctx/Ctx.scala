package com.kodeworks.doffapp.ctx

import com.kodeworks.doffapp.nlp.{MostUsedWordsImpl, MostUsedWords}
import com.kodeworks.doffapp.nlp.wordbank.{WordbankImpl, Wordbank}

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

trait CtxImpl extends Ctx
  with CfgImpl
  with PropImpl
  with FilesImpl
  with WordbankImpl
  with MostUsedWordsImpl
  with NlpImpl
  with DbImpl
  with ActorsImpl
  with HttpImpl

object Ctx extends CtxImpl