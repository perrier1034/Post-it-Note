package com.android.perrier1034.post_it_note.ui.views

import android.content.Context
import android.app.Activity
import android.graphics.Point
import android.util.{DisplayMetrics, AttributeSet}
import android.view.View.MeasureSpec
import android.view.WindowManager
import android.widget.LinearLayout
import com.android.perrier1034.post_it_note.ui.views.LinearLayoutEx.OnSoftKeyShownListener

object LinearLayoutEx {
  abstract class OnSoftKeyShownListener {
    def onSoftKeyShown(isShown: Boolean)
  }
}

class LinearLayoutEx(ctx: Context, attrs: AttributeSet) extends LinearLayout(ctx, attrs) {

  val mDisplayScale = getDisplayScale(getContext)
  var mListener: OnSoftKeyShownListener = null
  val DEBUG = true

  def this(context: Context) = this(context, null)

  def getDisplayScale(context: Context): Float = {
    val wm = context.getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager]
    val dm = new DisplayMetrics()
    wm.getDefaultDisplay.getMetrics(dm)
    dm.scaledDensity
  }

  def setOnSoftKeyShownListener(listener: OnSoftKeyShownListener) {
    mListener = listener
  }

  // density (比率)を取得する
  val density = getResources.getDisplayMetrics.density

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val activity = getContext.asInstanceOf[Activity]
    val display = activity.getWindowManager.getDefaultDisplay
    val  size = new Point()
    display.getSize(size)
    // (a)Viewの高さ
    val view = MeasureSpec.getSize(heightMeasureSpec);
    // (b)ステータスバーの高さ(48dpに決め打ち)
    val statusBar = (48f * density + 0.5f).asInstanceOf[Int]
    // (c)ディスプレイサイズ
    val screen = size.y
    // (a)-(b)-(c)>100ピクセルとなったらソフトキーボードが表示されてると判断
    //（ソフトキーボードはどんなものでも最低100ピクセルあると仮定）
    val diff = (screen - statusBar) - view
    if (mListener != null && mDisplayScale != 0) {
      val pixel =  (250 * mDisplayScale + 0.5f).asInstanceOf[Int]
      mListener.onSoftKeyShown(diff > pixel)
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

}
