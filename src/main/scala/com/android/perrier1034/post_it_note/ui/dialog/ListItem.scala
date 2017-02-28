package com.android.perrier1034.post_it_note.ui.dialog

/** Receiver class use this class as CallbackListener. */
abstract class ListItem(val label: String) extends Serializable {
  def execute()
}
