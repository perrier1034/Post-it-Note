package com.android.perrier1034.post_it_note.ui.views

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class HackyViewPager(ctx: Context, attrs: AttributeSet) extends ViewPager(ctx, attrs) {

  def this(context: Context) = this(context, null)

  var isLocked = false

  override def onInterceptTouchEvent(ev: MotionEvent): Boolean = {
    if (!isLocked) {
      try {
        return super.onInterceptTouchEvent(ev)
      } catch {
        case e: IllegalArgumentException =>
          e.printStackTrace()
          return false
      }
    }
    false
  }

  override def onTouchEvent(event: MotionEvent): Boolean = {
    if (!isLocked) {
      return super.onTouchEvent(event)
    }
    false
  }

  def toggleLock() {
    isLocked = !isLocked
  }

  def setLocked(b: Boolean ) {
    isLocked = b
  }
}
