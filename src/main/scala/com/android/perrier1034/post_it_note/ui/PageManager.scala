package com.android.perrier1034.post_it_note.ui

import com.android.perrier1034.post_it_note.model.PageRealm
import io.realm.{RealmChangeListener, RealmResults, Realm}

import scala.collection.JavaConversions._
import android.app.Activity
import android.content.Intent
import android.os.{Bundle, Handler}
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.view.View.OnClickListener
import android.view.{Menu, View}
import com.android.perrier1034.post_it_note.db.dao.{PageStore, NoteStore}
import com.android.perrier1034.post_it_note.receiver.AlarmBroadcastReceiver
import com.android.perrier1034.post_it_note.ui.PageFragment.Callback2Activity
import com.android.perrier1034.post_it_note.ui.adapters.MainPagerAdapter
import com.android.perrier1034.post_it_note.ui.dialog.ConfirmDialog
import com.android.perrier1034.post_it_note.ui.dialog.ConfirmDialog.ClickListener
import com.android.perrier1034.post_it_note.ui.fab.{FloatingActionButton, FloatingActionsMenu}
import com.android.perrier1034.post_it_note.ui.navigation.{IDrawerModel, ClickableDrawerModel, SectionDrawerModel}
import com.android.perrier1034.post_it_note.ui.views.LockableViewPager
import com.android.perrier1034.post_it_note.{EdamService, App, Constants, R}
import com.astuetz.PagerSlidingTabStrip
import com.evernote.client.android.EvernoteSession
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

final class PageManager extends BaseDrawerActivity {

  lazy val mDelayer = new Handler
  lazy val mFam = findViewById(R.id.fam).asInstanceOf[FloatingActionsMenu]
  lazy val mNoteFab = findViewById(R.id.fab_new_note).asInstanceOf[FloatingActionButton]
  lazy val mAddFloating = findViewById(R.id.fab_expand_menu_button).asInstanceOf[FloatingActionButton]
  lazy val mCheckListFab = findViewById(R.id.fab_new_list).asInstanceOf[FloatingActionButton]
  lazy val mTabStrip = findViewById(R.id.tab_strip_main).asInstanceOf[PagerSlidingTabStrip]
  lazy val mViewPager = findViewById(R.id.pager_main).asInstanceOf[LockableViewPager]
  val mCallback2Fragments = new mutable.HashMap[Int, PageManager.Callback2AttachedFragments]

  // mutable variables
  var initialPagePos = -1
  var initPagerDone = false

  def getCurPage = getAllPage(mViewPager.getCurrentItem)
  def getAllPage = mViewPager.getAdapter.asInstanceOf[MainPagerAdapter].getAllItems
  def getPageIndexById(id: Int) = getAllPage.indexOf(getAllPage.find( _.id == id ).get)

  val mCallback2Activity = new Callback2Activity {
    def onNoteClicked(id: Int, pageId: Int, cls: Class[_]) = startFloating(Some(id), pageId, cls)
    def onStartActionMode(menu: Menu) = {
      getMenuInflater.inflate(R.menu.action_mode_main, menu)
      lockViewPager()
      lockNavDrawer()
      hideTabs()
      setActionModeStatusBarColorLOLLIPOP()
      mFam.setVisibility(View.GONE)
    }
    def onActionModeFinished(position: Int) = {
      mFam.setVisibility(View.VISIBLE)
      unlockViewPager()
      unlockNavDrawer()
      showTabs()
      setStatusBarColorByPosition(getCurPage.pageColorPos)
      // move to the page
      if (position != -1) setCurrentPage(position)
    }
    def onFilteringStarted() = {
      mFam.setVisibility(View.GONE)
      hideTabs()
      lockViewPager()
      lockNavDrawer()
    }

    def onFilteringFinished() = {
      mFam.setVisibility(View.VISIBLE)
      showTabs()
      unlockViewPager()
      unlockNavDrawer()
    }
  }

  lazy val mOnPageChangeListener = new ViewPager.OnPageChangeListener {

    var mCurrPage: PageRealm = null

    def onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = {}
    def onPageSelected(position: Int) = {
      mCurrPage = getAllPage(position)
      getSupportActionBar.setTitle(mCurrPage.title)
      setToolBarColorByPosition(mCurrPage.pageColorPos)
      setStatusBarColorByPosition(mCurrPage.pageColorPos)
      val tbColor = Constants.PAGER_COLOR_MAPPING(mCurrPage.pageColorPos)(0)
      mTabStrip.setBackgroundColor(tbColor)
      mAddFloating.setColorNormal(tbColor)
    }
    def onPageScrollStateChanged(state: Int) = {
      if (ViewPager.SCROLL_STATE_IDLE == state) {
        val color = Constants.PAGER_COLOR_MAPPING(mCurrPage.pageColorPos)(0)
        mNoteFab.setColorNormal(color)
        mCheckListFab.setColorNormal(color)
      }
    }
  }

  override def onResume() = {
    super.onResume()
    val order = getIntent.getIntExtra(AlarmBroadcastReceiver.KEY_INTENT_PAGE_ORDER, -1)
    if (order > 0) initialPagePos = order
    if (initialPagePos != -1) {
      if (initPagerDone) {
        getIntent.removeExtra(AlarmBroadcastReceiver.KEY_INTENT_PAGE_ORDER)
        setCurrentPage(initialPagePos)
        initialPagePos = -1
      }
    }
  }

  override def onStart() = {
    val res = PageStore.allPagesRealmAsync
    res.addChangeListener(
      new RealmChangeListener() {
        def onChange() {
          initPager(res)
          initFab()
          App.Cache_Floating.pages = getAllPage
          res.removeChangeListener(this)
        }
      }
    )
    super.onStart()
  }

  override def init() = {
//    Future { PageStore.allPagesRealmAsync } onComplete {
//      case Success(notes) =>
//        initPager(notes)
//        initFab()
//        App.Cache_Floating.pages = getAllPage
//      case Failure(e) =>
//        App.toastShort("read error! Please reboot this app")
//        App.L(e.getMessage)
//    }

    // order by last update time
    if (App.getInstance.noteOrder == Constants.NOTE_ORDER_LAST_MODIFIED) {
      getDrawerListView.setItemChecked(1, true)
    } else {
      getDrawerListView.setItemChecked(2, true)
    }
  }

  override def contentsResId = R.layout.activity_component_pager

  def startFloating(id: Option[Int], pagId: Int, cls: Class[_]) = {
    App.Cache_Floating.pages = getAllPage

    val bu = new Bundle
    id foreach { bu.putInt(BaseFloating.KEY_ID, _) }
    bu.putInt(BaseFloating.KEY_PAGE_ID, pagId)

    val in = new Intent(this, cls)
    in.putExtra(BaseFloating.INTENT_INITIAL_ARGS, bu)

    startActivityForResult(in, BaseFloating.REQUEST_CODE)
  }

  def initFab() {
    val cb = (cls: Class[_]) => {
      mFam.collapse()
      App.Cache_Floating.pages = getAllPage
      startFloating(None, getCurPage.id.toInt, cls)
    }
    mNoteFab.setOnClickListener(new OnClickListener {
      def onClick(v: View) = cb(classOf[NoteEditActivity])
    })
    mCheckListFab.setOnClickListener(new OnClickListener {
      def onClick(v: View) = cb(classOf[CheckListActivity])
    })
  }

  def initPager(pages: Seq[PageRealm]) {
    def initTabStrip() {
      mTabStrip.setUnderlineHeight(2)
      mTabStrip.setIndicatorHeight(6)
      mTabStrip.setUnderlineColor(0xFF9E9E9E)
      mTabStrip.setAllCaps(false)
      mTabStrip.setIndicatorColor(0xffffffff)
      mTabStrip.setTextColor(0xffffffff)
      mTabStrip.setOnPageChangeListener(mOnPageChangeListener)
      mOnPageChangeListener.onPageSelected(0)
      mOnPageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE)
    }
    mViewPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager, pages))
    initTabStrip()
    mTabStrip.setViewPager(mViewPager)
    initPagerDone = true
    if (initialPagePos != -1) {
      setCurrentPage(initialPagePos)
      initialPagePos = -1
    }
  }


  def getCallback2Activity = mCallback2Activity

  def lockViewPager() = mViewPager.setPagingEnabled(false)
  def unlockViewPager() = mViewPager.setPagingEnabled(true)
  def hideTabs() = mTabStrip.setVisibility(View.GONE)
  def showTabs() = mTabStrip.setVisibility(View.VISIBLE)

  def addListenerFragment(pageId: Int, pf: PageFragment) = mCallback2Fragments.put(pageId, pf)
  def removeListenerFragment(pageId: Int) = mCallback2Fragments.remove(pageId)
  def reflectNoteOrder() = mCallback2Fragments foreach { _._2.onNoteOrderChanged() }

  override def genDrawerList: Vector[IDrawerModel] = {
    def id2Str(id: Int) = getText(id).asInstanceOf[String]
    val isLastModified = App.getInstance.noteOrder == Constants.NOTE_ORDER_LAST_MODIFIED
    val isIdOrder = !isLastModified
    Vector(
      // show "order"
      SectionDrawerModel(id2Str(R.string.nav_drawer_sort_section), true),

      //  order by last mod time
      ClickableDrawerModel(id2Str(R.string.nav_drawer_sort_update), None, isLastModified,
        (model: ClickableDrawerModel) => {
          if (!getDrawerListView.isItemChecked(1)) {
            getDrawerListView.setItemChecked(1, true)
            mDrawerLayout.closeDrawers()
            mDelayer.postDelayed(new Runnable {
              def run() = {
                App.getInstance.saveNoteOrderDesc
                reflectNoteOrder()
              }
            }, Constants.DRAWER_OPEN_DELAY)
          }
        }),

      // order by creation time
      ClickableDrawerModel(id2Str(R.string.nav_drawer_sort_create), None, isIdOrder,
        (model: ClickableDrawerModel) => {
          if (!getDrawerListView.isItemChecked(2)) {
            getDrawerListView.setItemChecked(2, true)
            mDrawerLayout.closeDrawers()
            mDelayer.postDelayed(new Runnable {
              def run() = {
                App.getInstance.saveNoteOrderAsc
                reflectNoteOrder()
              }
            }, Constants.DRAWER_OPEN_DELAY)

          }
        }),

      // rubbish
      SectionDrawerModel(id2Str(R.string.nav_drawer_setting_section), isShown=true),
      ClickableDrawerModel(id2Str(R.string.nav_drawer_setting_rubbish), Some(R.drawable.ic_action_delete),
        isShown=false,
        (model: ClickableDrawerModel) => {
          mDrawerLayout.closeDrawers()
          mDelayer.postDelayed(new Runnable {
            def run() = startActivity(new Intent(PageManager.this, classOf[RubbishActivity]))
          }, Constants.DRAWER_OPEN_DELAY)
        }),

      // page setting
      ClickableDrawerModel(id2Str(R.string.nav_drawer_setting_page), Some(R.drawable.ic_action_setting_page),
        isShown=false,
        (model: ClickableDrawerModel) => {
          mDrawerLayout.closeDrawers()
          mDelayer.postDelayed(new Runnable {
            def run() {
              App.Cache_PSA.pages = getAllPage
              startActivityForResult(new Intent(PageManager.this, classOf[PageSettingActivity]),
                PageSettingActivity.REQUEST_CODE)
            }
          }, Constants.DRAWER_OPEN_DELAY)
        }),

      // setting activity
      ClickableDrawerModel(id2Str(R.string.nav_drawer_setting_others), Some(R.drawable.ic_action_settings),
        isShown=false,
        (model: ClickableDrawerModel) => {
          mDrawerLayout.closeDrawers()
          mDelayer.postDelayed(new Runnable {
            def run() {
              startActivityForResult(new Intent(PageManager.this, classOf[SettingActivity]),
                SettingActivity.REQUEST_CODE)
            }
          }, Constants.DRAWER_OPEN_DELAY)
        }),

      // edam
      ClickableDrawerModel(id2Str(R.string.nav_drawer_edam), Some(R.drawable.edam_s), isShown=false,
        (model: ClickableDrawerModel) => {
          if (EdamService.isLoggedIn) { // do log out
            ConfirmDialog.newInstance(
              Some(id2Str(R.string.cdialog_navigation_edam_logout_title)),
              id2Str(R.string.cdialog_navigation_edam_logout),
              Some(R.drawable.edam),
              new ClickListener {
                def onClick() {
                  mDrawerLayout.closeDrawers()
                  mDelayer.postDelayed(new Runnable {
                    def run() = EdamService.logout
                  }, Constants.DRAWER_OPEN_DELAY)
                }
              }).show(getFragmentManager, "confDialog1")
          } else { // do log in
            ConfirmDialog.newInstance(
              Some(id2Str(R.string.cdialog_navigation_edam_login_title)),
              id2Str(R.string.cdialog_navigation_edam_login),
              Some(R.drawable.edam),
              new ClickListener {
                def onClick() {
                  mDrawerLayout.closeDrawers()
                  mDelayer.postDelayed(new Runnable {
                    def run() = EdamService.login(PageManager.this)
                  }, Constants.DRAWER_OPEN_DELAY)
                }
              }).show(getFragmentManager, "confDialog")
          }}
      )
    )
  }

  def killAllFragments() = getAdapter.removeAll(mViewPager)

  def getAdapter = mViewPager.getAdapter.asInstanceOf[MainPagerAdapter]

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    requestCode match {
      case SettingActivity.REQUEST_CODE =>
        if (resultCode == Activity.RESULT_OK) {
          val bun = data.getExtras
          if (bun.getBoolean(SettingActivity.KEY_BUNDLE_DB_IMPORTED)) {
            killAllFragments()

            val res = PageStore.allPagesRealmAsync
            res.addChangeListener(
              new RealmChangeListener() {
                def onChange() {
                  initPager(res)
                  getAdapter.notifyDataSetChanged()
                  setCurrentPage(0)
                  initFab()
                  App.Cache_Floating.pages = getAllPage
                  res.removeChangeListener(this)
                }
              }
            )

          }
        }

      case PageSettingActivity.REQUEST_CODE =>
        if (resultCode == Activity.RESULT_OK) {
          // set color
          val curPage = getCurPage
          getSupportActionBar.setTitle(curPage.title)
          val color = Constants.PAGER_COLOR_MAPPING(curPage.pageColorPos)(0)
          mToolbar.setBackgroundColor(color)
          mTabStrip.setBackgroundColor(color)
          setStatusBarColorByPosition(curPage.pageColorPos)
          mAddFloating.setColorNormal(color)
          mNoteFab.setColorNormal(color)
          mCheckListFab.setColorNormal(color)

          // restart pages
          killAllFragments()
          getAdapter.setItems(App.Cache_PSA.pages)
          mTabStrip.setViewPager(mViewPager)
          getAdapter.restart()
          setCurrentPage(0)
        }

      case BaseFloating.REQUEST_CODE =>
        if (resultCode == Activity.RESULT_OK) {
          val bu = data.getExtras
          if (bu.getBoolean(CheckListActivity.BUNDLE_KEY_DELETE_CLICKED, false)) {
            Snackbar.make(getContainer,R.string.snack_log_deleted, Snackbar.LENGTH_LONG)
              .setAction("UNDO", new View.OnClickListener() { def onClick(v: View ) =
                NoteStore.resurrectFromRubbishRealm(App.Cache_Floating.note)
            }).show()
          }
          val id = bu.getInt(CheckListActivity.BUNDLE_KEY_SELECTED_PAGE_ID, -1)
          if (id != -1) setCurrentPage(getPageIndexById(id))
        }
      case EvernoteSession.REQUEST_CODE_LOGIN =>
        if (resultCode == Activity.RESULT_OK) {
          App.toastShort("login success")
        } else {
          App.toastShort("login failed")
        }
    }
  }

  def setCurrentPage(pos: Int) = mViewPager.setCurrentItem(pos, true)

}

object PageManager {
  trait Callback2AttachedFragments {
    def onNoteOrderChanged()
  }
}
