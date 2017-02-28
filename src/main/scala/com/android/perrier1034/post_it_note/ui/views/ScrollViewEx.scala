package com.android.perrier1034.post_it_note.ui.views

import android.util.AttributeSet
import android.content.Context
import android.view.MotionEvent
import android.widget.ScrollView

class ScrollViewEx(ctx: Context, attrs: AttributeSet) extends ScrollView(ctx, attrs) {

  val DISTANCE = 20f
  var downX = 0f
  var downY = 0f

  def this(context: Context) = this(context, null)

  /**
   * return value ...
   * true  -> kill child and then this.onTouchEvent()
   * false -> propagate event to child
   */
  override def onInterceptTouchEvent(event: MotionEvent): Boolean = {

    // We have to call below to do scrolling properly.
    // So anyway call it and alloc the return value
    val b = super.onInterceptTouchEvent(event)

    // save touched position when "ACTION_DOWN" !
    if (event.getAction == MotionEvent.ACTION_DOWN) {
      downX = event.getX
      downY = event.getY
    }

    // calc distance
    if (event.getAction == MotionEvent.ACTION_UP) {
      return (Math.abs(event.getY - downY) > DISTANCE) || (Math.abs(event.getX - downX) > DISTANCE)
    }

    b
  }
}
