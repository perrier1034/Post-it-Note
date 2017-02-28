package com.android.perrier1034.post_it_note.ui

import android.graphics.drawable.ColorDrawable
import android.os.{Build, Bundle}
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.{View, ViewGroup, WindowManager}
import com.android.perrier1034.post_it_note.{Constants, R}

abstract class BaseActivity extends AppCompatActivity {

  lazy val mToolbar = findViewById(R.id.toolbar).asInstanceOf[Toolbar]
  lazy val mStatusBar = findViewById(R.id.status_bar)

  protected def contentsResId: Int
  protected def init()

  override final def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_base)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
      getWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
    val container = findViewById(R.id.container).asInstanceOf[ViewGroup]
    initToolbar(container)
    View.inflate(this, contentsResId, container)
    init()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
      getWindow.setBackgroundDrawable(new ColorDrawable(Constants.STATUS_BAR_COLOR_BASE))
  }

  def initToolbar(container: ViewGroup) = {
    setSupportActionBar(mToolbar)
    mToolbar.setBackgroundColor(Constants.TOOL_BAR_COLOR_BASE)
  }

  def getToolbar: Toolbar = mToolbar

  def setStatusBarColorByPosition(_pos: Int) {
    val pos: Int = if (_pos >= 0) _pos else 14
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow.setStatusBarColor(Constants.PAGER_COLOR_MAPPING(pos)(3))
    }
    else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
      mStatusBar.setBackgroundColor(Constants.PAGER_COLOR_MAPPING(pos)(0))
    }
  }

  /**
   * 5.0++
   */
  def setActionModeStatusBarColorLOLLIPOP() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      getWindow.setStatusBarColor(Constants.STATUS_BAR_COLOR_ACTION_MODE)
  }

}