package com.android.perrier1034.post_it_note.ui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.graphics.{Canvas, Rect}
import android.support.v7.widget.RecyclerView

object DividerAdder {
  private val ATTRS: Array[Int] = Array[Int](android.R.attr.listDivider)
}

class DividerAdder extends RecyclerView.ItemDecoration {
  private var mDivider: Drawable = null

  def this(context: Context) {
    this()
    val array: TypedArray = context.obtainStyledAttributes(DividerAdder.ATTRS)
    mDivider = array.getDrawable(0)
    array.recycle
  }

  @SuppressWarnings(Array("deplicated"))
  override def getItemOffsets(outRect: Rect, itemPosition: Int, parent: RecyclerView) {
    outRect.set(0, 0, 0, mDivider.getIntrinsicHeight)
  }

  override def onDraw(c: Canvas, parent: RecyclerView) {
    drawVertical(c, parent)
  }

  def drawVertical(c: Canvas, parent: RecyclerView) {
    val left: Int = parent.getPaddingLeft
    val right: Int = parent.getWidth - parent.getPaddingRight
    val childCount: Int = parent.getChildCount

    for (i <- 0 until parent.getChildCount) {
      val child = parent.getChildAt(i)
      val params = child.getLayoutParams.asInstanceOf[RecyclerView.LayoutParams]
      val top = child.getBottom + params.bottomMargin
      val bottom = top + mDivider.getIntrinsicHeight
      mDivider.setBounds(left, top, right, bottom)
      mDivider.draw(c)
    }
  }
}