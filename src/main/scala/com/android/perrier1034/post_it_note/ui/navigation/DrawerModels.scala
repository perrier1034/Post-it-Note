package com.android.perrier1034.post_it_note.ui.navigation

sealed abstract class IDrawerModel {
  val label: String
  val isEnabled: Boolean
  val isShown: Boolean
}

case class SectionDrawerModel(label: String, isShown: Boolean) extends IDrawerModel {
  val POS_SECTION_ORDER_NOTE = 3
  val isEnabled = false
}

case class ClickableDrawerModel(label: String, iconResId: Option[Int], isShown: Boolean,
                                callback: ClickableDrawerModel => Unit) extends IDrawerModel {
  val isEnabled = true
  // This is called in Adapter
  def call() = callback(this)
}
