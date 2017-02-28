package com.android.perrier1034.post_it_note
import java.io.File
import java.util.UUID

import android.app.Application
import android.os.{Environment, Handler, Looper}
import android.content.Context
import android.preference.PreferenceManager
import android.util.{DisplayMetrics, Log}
import android.view.WindowManager
import android.widget.Toast
import com.android.perrier1034.post_it_note.db.dao.PageStore
import com.android.perrier1034.post_it_note.model.{NoteRealm, PageRealm}
import io.realm.Realm

/**
 * @author perrier1034
 */
object App {

  val NAME = "Post-it-Note"
  val TAG = "Post-it-Note"
  val AUTHOR = "perrier1034"
  val DB_NAME = "Application.db"

  // value will be injected when onCreate
  var sInstance: App = null

  // used to show toast from worker thread
  lazy val toastHandler = new Handler(Looper.getMainLooper)

  def getInstance: App = sInstance

  object Cache_Floating {
    var pages: Seq[PageRealm] = null
    var note: NoteRealm = null
  }

  object Cache_PSA {
    var curPage: Option[PageRealm] = None
    var pages: Seq[PageRealm] = null
  }

  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _ => bytes.map("%02x".format(_)).mkString(sep.get)
    }
  }

  def getDisplayScale(context: Context): Float = {
    val wm = context.getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager]
    val dm = new DisplayMetrics
    wm.getDefaultDisplay.getMetrics(dm)
    dm.scaledDensity
  }

  // toast
  def toastShort(str: String) = Toast.makeText(App.getInstance, str, Toast.LENGTH_SHORT).show()
  def toastShort(resId: Int) = Toast.makeText(App.getInstance, resId, Toast.LENGTH_SHORT).show()
  def toastShortInUI(str: String) = toastHandler.post(new Runnable() {
    def run() = Toast.makeText(App.getInstance, str, Toast.LENGTH_SHORT).show()
  })
  def toastShortInUI(resId: Int) = toastHandler.post(new Runnable() {
    def run() = Toast.makeText(App.getInstance, resId, Toast.LENGTH_SHORT).show()
  })
  def toastLongInUI(str: String) = toastHandler.post(new Runnable() {
    def run() = Toast.makeText(App.getInstance, str, Toast.LENGTH_LONG).show()
  })

  // log
  def L(o: AnyRef, str: String) = Log.d(App.TAG, String.format("@%s - %s", o.toString, str))
  def L(str: String) = Log.d(TAG, str)
  def Le(o: AnyRef, e: Exception) = Log.e(App.TAG, String.format("@%s - %s", o.toString, e.toString))
  def showStackTrace() = {
    Thread.currentThread.getStackTrace
    Log.i("App", "", new Throwable)
  }

  // storage
  def getStorageDir = new File(storageDirName, App.NAME)
  def storageDirName = Environment.getExternalStorageDirectory
  def isStorageAvailable: Boolean = Environment.getExternalStorageState == Environment.MEDIA_MOUNTED
  val imageDirName = storageDirName + ("/" + NAME + "/image/")

  def makeUUIDString: String = UUID.randomUUID.toString

  var sUniqueId = 0
  def genUniqueNum = {
    sUniqueId += 1
    sUniqueId
  }

}

class App extends Application {

  override def onTerminate() {
    Realm.getInstance(this).close()
    super.onTerminate()
  }

  var start = System.currentTimeMillis()

  override def onCreate() {
    var end = System.currentTimeMillis()
    App.L((end - start) + "millis (App instance created to onCreate called)")
    App.sInstance = this

    start = System.currentTimeMillis()
    end = System.currentTimeMillis()
    App.L((end - start) + "millis (during AA.initialize())")

    val b = getSharedPreferences.getBoolean("initial_boot", false)
    if (!b) {
      getSharedPreferences.edit.putBoolean("initial_boot", true).commit
      PageStore.createPrimaryPageRealm()
    }
    end = System.currentTimeMillis()
  }
  def noteOrderRealm = {
    getSharedPreferences.getString("note_order", Constants.NOTE_ORDER_LAST_MODIFIED)
  }

  def setSortLastModifiedRealm() =
    getSharedPreferences.edit.putString("note_order", Constants.NOTE_ORDER_LAST_MODIFIED).commit
  def setSortIdRealm()  =
    getSharedPreferences.edit.putString("note_order", Constants.NOTE_ORDER_id).commit

  def noteOrder: String = getSharedPreferences.getString("note_order", "_id DESC")
  def saveNoteOrderAsc = getSharedPreferences.edit.putString("note_order", "_id ASC").commit
  def saveNoteOrderDesc = getSharedPreferences.edit.putString("note_order", "_id DESC").commit

  def getSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

  def dp2px(dp: Int): Int = (dp * getResources.getDisplayMetrics.density).toInt
  def px2dp(px: Int): Int = (px / getResources.getDisplayMetrics.density).toInt

  def isTablet(context: Context): Boolean = {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager]
    val display = windowManager.getDefaultDisplay

    val  metrics = new DisplayMetrics()
    display.getMetrics(metrics)

    val inchX = metrics.widthPixels / metrics.xdpi
    val inchY = metrics.heightPixels / metrics.ydpi
    val inch = Math.sqrt((inchX * inchX) + (inchY * inchY))

    inch > 6
  }

}