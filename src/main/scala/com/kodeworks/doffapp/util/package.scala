package com.kodeworks.doffapp

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

package object util {

  implicit class RichFuture[T](f: Future[T]) {
    def mapAll[Target](m: Try[T] => Target)(implicit ec: ExecutionContext): Future[Target] = {
      val promise = Promise[Target]()
      f.onComplete { r => promise success m(r) }(ec)
      promise.future
    }

    def flatMapAll[Target](m: Try[T] => Future[Target])(implicit ec: ExecutionContext): Future[Target] = {
      val promise = Promise[Target]()
      f.onComplete { r => m(r).onComplete { z => promise complete z }(ec) }(ec)
      promise.future
    }
  }

}
