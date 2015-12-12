package com.kodeworks.doffapp.util

import java.time.{OffsetDateTime, LocalDateTime, ZoneOffset}

trait Util {
  def now: OffsetDateTime = LocalDateTime.now.atOffset(ZoneOffset.UTC)
}
