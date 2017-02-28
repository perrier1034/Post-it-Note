package com.android.perrier1034.post_it_note.ui

import android.content.res.Configuration
import android.os.{Build, Bundle}
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.{ActionBarDrawerToggle, AppCompatActivity}
import android.support.v7.widget.Toolbar
import android.view.{WindowManager, MenuItem, View, ViewGroup}
import android.widget.ListView
import com.android.perrier1034.post_it_note.{App, Constants, R}
import com.android.perrier1034.post_it_note.ui.navigation.{IDrawerModel, DrawerAdapter}

abstract class BaseDrawerActivity extends AppCompatActivity {

  lazy val mDrawerLayout = findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
  lazy val mToolbar = findViewById(R.id.toolbar).asInstanceOf[Toolbar]
  lazy val mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.app_name, R.string.app_name)
  lazy val mStatusBar = findViewById(R.id.status_bar)
  lazy val mDrawerListView = mDrawerLayout.findViewById(R.id.drawer_list).asInstanceOf[ListView]
  lazy val mContainerView = findViewById(R.id.container).asInstanceOf[ViewGroup]

  def init()
  def contentsResId: Int
  def genDrawerList: Seq[IDrawerModel]

  override final def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_base_drawer)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
      getWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
      mStatusBar.setVisibility(View.VISIBLE)
    }
    View.inflate(this, contentsResId, mContainerView)
    initToolbar(mContainerView)
    initNavDrawer()
    init()
  }

  def initNavDrawer() = {
    mDrawerListView.setAdapter(new DrawerAdapter(genDrawerList, this))
  }

  def getContainer = mContainerView

  def getDrawerListView = mDrawerListView

  def initToolbar(container: ViewGroup) {
    setSupportActionBar(mToolbar)
    mDrawerToggle.setDrawerIndicatorEnabled(true)
    mDrawerLayout.setDrawerListener(mDrawerToggle)
  }

  def getToolbar: Toolbar = mToolbar

  def lockNavDrawer() = mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

  def unlockNavDrawer() = mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    if (mDrawerToggle != null) {
      mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    } else {
      super.onOptionsItemSelected(item)
    }

  override protected def onPostCreate(savedInstanceState: Bundle) {
    super.onPostCreate(savedInstanceState)
    if (mDrawerToggle != null) mDrawerToggle.syncState()
  }

  override def onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig)
  }

  def setStatusBarColorByPosition(pos: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow.setStatusBarColor(Constants.PAGER_COLOR_MAPPING(pos)(3))
    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
      mStatusBar.setBackgroundColor(Constants.PAGER_COLOR_MAPPING(pos)(0))
    }
  }

  def setToolBarColorByPosition(pos: Int) {
    mToolbar.setBackgroundColor(Constants.PAGER_COLOR_MAPPING(pos)(0))
  }

  /**
   * 5.0++
   */
  def setActionModeStatusBarColorLOLLIPOP() = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      getWindow.setStatusBarColor(Constants.STATUS_BAR_COLOR_ACTION_MODE)
  }

}
