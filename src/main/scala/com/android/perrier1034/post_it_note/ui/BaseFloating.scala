package com.android.perrier1034.post_it_note.ui

import java.util.{Calendar, TimeZone}

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.{Intent, Context}
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem.OnMenuItemClickListener
import android.view.inputmethod.InputMethodManager
import android.view.{WindowManager, Menu, MenuItem, View}
import android.widget.AdapterView.OnItemSelectedListener
import android.widget._
import com.android.perrier1034.post_it_note._
import com.android.perrier1034.post_it_note.db.dao.NoteStore
import com.android.perrier1034.post_it_note.model.NoteRealm
import com.android.perrier1034.post_it_note.ui.dialog.{DatePickerDialog, ConfirmDialog}
import com.android.perrier1034.post_it_note.ui.dialog.ConfirmDialog.ClickListener
import com.android.perrier1034.post_it_note.util.AlarmUtil

object BaseFloating {
  val KEY_DEFAULT_BODY = "0"
  val KEY_DEFAULT_TITLE = "1"
  val KEY_ID = "2"
  val KEY_PAGE_ID = "3"
  val KEY_HAS_ALARM = "4"
  val KEY_IS_CHECK = "5"
  val KEY_LIST_BODIES = "6"
  val KEY_LIST_CHECK_STATES = "7"
  val KEY_ORIG_IM_NAME_ARRAY = "a"
  val KEY_THUMB_NAME_ARRAY = "b"
  val INTENT_INITIAL_ARGS = "c"
  val REQUEST_CODE = 0x4
}

/**
 * define `common` things or abstract methods
 */
abstract class BaseFloating extends AppCompatActivity {

  lazy val mInitialArg = getIntent.getBundleExtra(BaseFloating.INTENT_INITIAL_ARGS)
  var curDate: Option[(Int, Int, Int)] = None
  var mSelectedPageId = 0

  // for child class
  def init()
  def contentResId: Int
  def serveActivityInstance: Activity
  def backPressedCallback()
  def serveNotes2Edam: Option[Seq[NoteRealm]]

  final def isNewNote = getID <= 0
  final def getID: Int = mInitialArg.getInt(BaseFloating.KEY_ID, -1)
  final def getPageId: Int = mInitialArg.getInt(BaseFloating.KEY_PAGE_ID)

  // methods
  override final def onCreate(savedInstanceState: Bundle) = {
    initWindow()
    super.onCreate(savedInstanceState)
    setContentView(contentResId)
    mSelectedPageId = getPageId

    // ------------ common views ---------------
    // tool bar
    val tb = serveActivityInstance.findViewById(R.id.toolbar).asInstanceOf[Toolbar]
    tb.setBackgroundColor(Constants.TOOL_BAR_COLOR_BASE)
    setSupportActionBar(tb)
    getSupportActionBar.setDisplayShowTitleEnabled(false)
    // spinner
    val sp = tb.findViewById(R.id.spinner_page).asInstanceOf[Spinner]
    val adapter = new ArrayAdapter[String](this, android.R.layout.simple_spinner_item)
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    val pages = App.Cache_Floating.pages
    pages foreach { page => adapter.add(page.title) }
    sp.setAdapter(adapter)
    sp.setOnItemSelectedListener(new OnItemSelectedListener {
      def onItemSelected(a: AdapterView[_], v: View, pos: Int, id: Long) = mSelectedPageId = pages(pos).id.toInt
      def onNothingSelected(arg0: AdapterView[_]) = {}
    })
    sp.setSelection(pages.indexOf(pages.find(_.id == getPageId).get))
    // ------------ common views end ---------------

    keepOrientation()

    init()
  }

  override def onDestroy() = {
    setOrientationAuto()
    super.onDestroy()
  }

  def setOrientationAuto() = {
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
  }

  private def keepOrientation() = {
    val ori = getResources.getConfiguration.orientation
    if (ori == Configuration.ORIENTATION_LANDSCAPE)
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    else if (ori == Configuration.ORIENTATION_PORTRAIT)
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val edam = menu.findItem(R.id.f_export_evernote)
    val alarmAdd = menu.findItem(R.id.f_alarm_set)
    val alarmClear  = menu.findItem(R.id.f_alarm_clear)
    if (EdamService.isLoggedIn) edam.setVisible(true)
    alarmAdd.setVisible(!hasAlarm)
    alarmClear.setVisible(hasAlarm)

    alarmClear.setOnMenuItemClickListener(new OnMenuItemClickListener {
      override def onMenuItemClick(menuItem: MenuItem): Boolean = {
        alarmAdd.setVisible(true)
        alarmClear.setVisible(false)
        if (!isNewNote) {
          val note = App.Cache_Floating.note
          AlarmUtil.clear(note.id)
          note.alarmTime = null
        }
        App.toastShortInUI(R.string.cleared_alarm)
        curDate = None
        true
      }
    })

    alarmAdd.setOnMenuItemClickListener(new OnMenuItemClickListener {
      override def onMenuItemClick(menuItem: MenuItem): Boolean = {
        val cal = Calendar.getInstance
        cal.setTimeInMillis(System.currentTimeMillis)
        cal.setTimeZone(TimeZone.getDefault)
        val dateListener = new DatePickerDialog.Listener() {
          override def call(y: Int, m: Int, d: Int) {
            curDate = Some((y, m, d))
            alarmAdd.setVisible(false)
            alarmClear.setVisible(true)
            App.toastShort((m + 1) + " / " + d)
          }
        }
        DatePickerDialog.newInstance(dateListener)
          .show(BaseFloating.this.getSupportFragmentManager, "dp")

        true
      }
    })
    true
  }

  // Only CheckListActivity will override this method
  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.f_export_evernote =>
        serveNotes2Edam match {
          case Some(notes) => EdamService.saveNotesRealm(notes, getSupportFragmentManager, None)
          case None => App.toastShort("Note is empty!")
        }
        return true
    }
    super.onOptionsItemSelected(item)
  }


  // for NoteEditActivity, "always" is not needed
  def setInputMethodVisible() =
    getWindow.setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    )

  def showInputMethod(v: View) = {
    val imm = App.getInstance.getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
    imm.showSoftInput(v, InputMethodManager.SHOW_FORCED)
  }

  def setInputMethodInvisible() =
    getWindow.setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    )

  def hasAlarm = if (isNewNote) false else {
    val note = App.Cache_Floating.note
    !TextUtils.isEmpty(note.alarmTime)
  }

  override def onBackPressed() = backPressedCallback()

  final def hideKeyboard() = {
    if (getCurrentFocus != null) {
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
      imm.hideSoftInputFromWindow(getCurrentFocus.getWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
  }

  final def prepareActivityResult() {
    if (mSelectedPageId != getPageId) {
      val in = new Intent
      val bun = new Bundle
      if (mSelectedPageId != getPageId) {
        bun.putInt(CheckListActivity.BUNDLE_KEY_SELECTED_PAGE_ID, mSelectedPageId)
      }
      in.putExtras(bun)
      setResult(Activity.RESULT_OK, in)
    }
  }

  /** called when back pressed in spite of the fact that check list is empty */
  final def showOnEmptyConfirmDialog() {
    ConfirmDialog.newInstance(None, getText(R.string.cdialog_warning_empty_note).toString, None,
      new ClickListener { override def onClick() = {
        NoteStore.moveToRubbishRealm(App.Cache_Floating.note)
        prepareActivityResult()
        finish()
      }}).show(getFragmentManager, "confDialog")
  }

  final protected def initWindow() {
    val display = getWindowManager.getDefaultDisplay
    val point = new Point
    display.getSize(point)
    val h = point.y
    val w = point.x
    if (h > w)
      getWindow.setLayout((w * .93).asInstanceOf[Int], (h * .9).asInstanceOf[Int])
    else
      getWindow.setLayout((w * .9).asInstanceOf[Int], (h * .9).asInstanceOf[Int])
  }

  final protected def belongChanged = mSelectedPageId != getPageId

  final def hideKeyBoard() {
    if (getCurrentFocus != null) {
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
      imm.hideSoftInputFromWindow(getCurrentFocus.getWindowToken, 0)
    }
  }

}