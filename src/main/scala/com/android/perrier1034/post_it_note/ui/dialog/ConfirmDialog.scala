package com.android.perrier1034.post_it_note.ui.dialog

import java.io.Serializable

import android.app.{Dialog, DialogFragment}
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.support.v7.app.AlertDialog

object ConfirmDialog {

  def newInstance(title: Option[String], msg: String, iconResId: Option[Int], lis: ClickListener) = {
    val instance = new ConfirmDialog
    val bun = new Bundle
    bun.putString("msg", msg)
    bun.putSerializable("listener", lis)
    title foreach { bun.putString("title", _) }
    iconResId foreach { bun.putInt("icon", _)}
    instance.setArguments(bun)
    instance
  }

  trait ClickListener extends Serializable {
    def onClick()
  }

}

class ConfirmDialog extends DialogFragment {

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {

    val builder = new AlertDialog.Builder(getActivity)
      .setMessage(getArguments.getString("msg"))
      .setPositiveButton("OK", new OnClickListener {
      override def onClick(dialogInterface: DialogInterface, i: Int) =
        getArguments.getSerializable("listener").asInstanceOf[ConfirmDialog.ClickListener].onClick()
    })

    Option(getArguments.getString("title")) foreach { builder.setTitle(_) }
    val icon = getArguments.getInt("icon", 0)
    if (icon > 0) builder.setIcon(icon)

    builder.create
  }
}