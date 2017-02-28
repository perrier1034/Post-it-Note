package com.android.perrier1034.post_it_note.ui.dialog

import android.app.{Activity, Dialog, DialogFragment}
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.{Drawable, RippleDrawable, ShapeDrawable, StateListDrawable}
import android.os.{Build, Bundle}
import android.support.v7.app.AlertDialog
import android.view.{LayoutInflater, View}
import android.widget.{FrameLayout, GridLayout}
import com.android.perrier1034.post_it_note.{Constants, R}

/**
 * @author Aidan Follestad (afollestad)
 */
object ColorChooserDialog {

  trait Callback {
    def onColorSelection(index: Int, color: Int, darker: Int)
  }

}

class ColorChooserDialog extends DialogFragment with View.OnClickListener {
  private var mCallback: ColorChooserDialog.Callback = null

  def onClick(v: View) {
    if (v.getTag != null) {
      val index = v.getTag.asInstanceOf[Integer]
      mCallback.onColorSelection(index, Constants.PAGER_COLOR_MAPPING(index)(0), shiftColor(Constants.PAGER_COLOR_MAPPING(index)(0)))
      dismiss()
    }
  }

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val view = LayoutInflater.from(getActivity).inflate(R.layout.dialog_color_chooser, null)
    val dialog = new AlertDialog.Builder(getActivity)
      .setTitle(R.string.color_chooser)
      .setView(view)
      .create
    val grid = view.findViewById(R.id.grid).asInstanceOf[GridLayout]
    val preselect = getArguments.getInt("preselect", -1)
    for (i <- 0 until grid.getChildCount) {
      val child = grid.getChildAt(i).asInstanceOf[FrameLayout]
      child.setTag(i)
      child.setOnClickListener(this)
      child.getChildAt(0).setVisibility(if (preselect == i) View.VISIBLE else View.GONE)
      val selector: Drawable = createSelector(Constants.PAGER_COLOR_MAPPING(i)(0))
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val states = Array[Array[Int]](Array[Int](-android.R.attr.state_pressed), Array[Int](android.R.attr.state_pressed))
        val colors = Array[Int](shiftColor(Constants.PAGER_COLOR_MAPPING(i)(0)), Constants.PAGER_COLOR_MAPPING(i)(0))
        val rippleColors: ColorStateList = new ColorStateList(states, colors)
        setBackgroundCompat(child, new RippleDrawable(rippleColors, selector, null))
      } else {
        setBackgroundCompat(child, selector)
      }
    }
    dialog
  }

  private def setBackgroundCompat(view: View, d: Drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) view.setBackground(d)
    else view.setBackgroundDrawable(d)
  }

  private def shiftColor(color: Int): Int = {
    val hsv = new Array[Float](3)
    Color.colorToHSV(color, hsv)
    hsv(2) *= 0.9f
    Color.HSVToColor(hsv)
  }

  private def createSelector(color: Int): Drawable = {
    val coloredCircle = new ShapeDrawable(new OvalShape)
    coloredCircle.getPaint.setColor(color)
    val darkerCircle = new ShapeDrawable(new OvalShape)
    darkerCircle.getPaint.setColor(shiftColor(color))
    val stateListDrawable = new StateListDrawable
    stateListDrawable.addState(Array[Int](-android.R.attr.state_pressed), coloredCircle)
    stateListDrawable.addState(Array[Int](android.R.attr.state_pressed), darkerCircle)
    stateListDrawable
  }

  def show(context: Activity, preselect: Int, callback: ColorChooserDialog.Callback) {
    mCallback = callback
    val args = new Bundle
    args.putInt("preselect", preselect)
    setArguments(args)
    show(context.getFragmentManager, "COLOR_SELECTOR")
  }
}