package com.android.perrier1034.post_it_note.ui.dialog

import android.app.{Dialog, DialogFragment}
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.text.{Editable, TextWatcher}
import android.view.{LayoutInflater, WindowManager}
import android.widget.Toast
import com.android.perrier1034.post_it_note.R

/**
 * This is simply used to receive String from user and send it to Activity.
 */
object EditTextDialog {

  def newInstance(title: Option[String] = None,
                  hint: Option[String] = None,
                  defaultValue: Option[String] = None,
                  limit: Option[Int] = None,
                  lis: ClickListener) = {

    val instance = new EditTextDialog
    val bun = new Bundle
    title foreach { bun.putString("title", _) }
    hint foreach { bun.putString("hint", _) }
    defaultValue foreach { bun.putString("default", _) }
    limit foreach { bun.putInt("limit", _) }
    bun.putSerializable("listener", lis)
    instance.setArguments(bun)
    instance
  }

  trait ClickListener extends Serializable {
    def onClick(str: String)
  }

}

class EditTextDialog extends DialogFragment {

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {

    val view = LayoutInflater.from(getActivity).inflate(R.layout.dialog_component_edittext, null)
    val editText = view.findViewById(R.id.edit_text).asInstanceOf[AppCompatEditText]

    getHint foreach { editText.setHint(_)}

    def inputIsValid(s: CharSequence): Boolean = {
      if ("" == s) {
        editText.setError("Empty!")
        return false
      }
      if (s.length > getArguments.getInt("limit")) {
        editText.setError(getText(R.string.edialog_warnimg_maxlen).toString)
        return false
      }
      true
    }

    val builder = new AlertDialog.Builder(getActivity).setView(view)
      .setPositiveButton("OK", new OnClickListener {
      override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
        val in = editText.getText.toString
        if (inputIsValid(in)) {
          getArguments.getSerializable("listener").asInstanceOf[EditTextDialog.ClickListener] .onClick(in)
        }
      }
    })

    editText.addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = {}
      override def afterTextChanged(s: Editable): Unit = {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = {
        inputIsValid(s)
      }
    })

    title foreach { builder.setTitle(_) }

    getDefaultValue foreach { x =>
      editText.setText(x)
      editText.setSelection(x.length)
    }

    val dialog = builder.create()
    dialog.getWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    dialog
  }

  def getDefaultValue = Option(getArguments.getString("default"))
  def title = Option(getArguments.getString("title"))
  def getHint = Option(getArguments.getString("hint"))

}