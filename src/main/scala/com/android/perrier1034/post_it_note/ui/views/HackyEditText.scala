package com.android.perrier1034.post_it_note.ui.views

import android.graphics.Canvas
import android.os.{Looper, Handler}
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.android.perrier1034.post_it_note.App

class HackyEditText(ctx: Context, attrs: AttributeSet) extends AppCompatEditText(ctx, attrs) {

  def this(ctx: Context) = this(ctx, null)

  var isTarget = false
  setWillNotDraw(false)

  override def onDraw(canvas: Canvas ) {
    super.onDraw(canvas)

    if (isTarget) {
      isTarget = false
      requestFocus()
      // setShowSoftInputOnFocus(true)

      if (isInputMethodTarget) {
        showKeyBoard()
      } else{
        // ugly hack but I have no choice.
        // this occur when CheckListAdapter.appendNewRow() called on creating new check-list-note
        new Thread(new Runnable() {
          override def run() = {
            new Handler(Looper.getMainLooper).postDelayed(new Runnable() {
              def run() = showKeyBoard()
            }, 50)
          }
        }).start()
      }
    }
  }

  def showKeyBoard() {
    val imm = getContext.getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
    imm.showSoftInput(this, InputMethodManager.SHOW_FORCED)
  }

}
