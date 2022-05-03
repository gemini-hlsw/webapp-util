package japgolly.webapputil.general

import japgolly.microlibs.utils.SafeBool

sealed trait Enabled extends SafeBool.WithBoolOps[Enabled] {
  override final def companion = Enabled
}

case object Enabled extends Enabled with SafeBool.Object[Enabled] {
  override def positive = Enabled
  override def negative = Disabled
}

case object Disabled extends Enabled
