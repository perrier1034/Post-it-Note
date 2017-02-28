package com.android.perrier1034.post_it_note.ui.views

import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.content.Context
import android.view.MotionEvent

class LockableViewPager(ctx: Context, attrs: AttributeSet) extends ViewPager(ctx, attrs) {

  def this(context: Context) = this(context, null)

  var mImPagingEnabled = true

  override def onTouchEvent(event: MotionEvent): Boolean =
    mImPagingEnabled && super.onTouchEvent(event)

  override def onInterceptTouchEvent(event: MotionEvent ): Boolean =
    mImPagingEnabled && super.onInterceptTouchEvent(event)

  def setPagingEnabled(b: Boolean ) {
    this.mImPagingEnabled = b
  }

}
