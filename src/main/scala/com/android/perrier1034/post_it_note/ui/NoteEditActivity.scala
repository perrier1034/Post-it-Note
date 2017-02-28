package com.android.perrier1034.post_it_note.ui

import java.io.File

import java.util.UUID

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os._
import android.provider.MediaStore
import android.text.{Editable, TextUtils, TextWatcher}
import android.view._
import android.widget._
import com.android.perrier1034.post_it_note.db.dao.NoteStore
import com.android.perrier1034.post_it_note.model.{NoteRealm, ImageInfo}
import com.android.perrier1034.post_it_note.ui.dialog._
import com.android.perrier1034.post_it_note.ui.views.LinearLayoutEx
import com.android.perrier1034.post_it_note.util.{IOUtil, AlarmUtil, ImageUtil}
import com.android.perrier1034.post_it_note.{Background, EdamService, App, R}
import com.android.perrier1034.post_it_note.util.SeqUtil._
import com.evernote.client.android.EvernoteSession
import com.squareup.picasso.Picasso
import io.realm.Realm

import scala.collection.mutable.ListBuffer

object NoteEditActivity {
  val REQUEST_SELECT_IMAGE_KK = 0x5
  val REQUEST_SELECT_IMAGE = 0x6
  val REQUEST_CAPTURE_IMAGE = 0x7
  val INTENT_CURRENT_CAPTURE_IMAGE_NAME = "iccin"
}

final class NoteEditActivity extends BaseFloating {
  // mutable variables
  var isImagesChanged = false
  var mCurrCaptureFilename = ""

  lazy val imageContainer = findViewById(R.id.im_container).asInstanceOf[ViewGroup]
  lazy val horizontal = findViewById(R.id.horizontal_scroll).asInstanceOf[ViewGroup]
  lazy val mainEditText = findViewById(R.id.f_note_main_edittext).asInstanceOf[EditText]
  lazy val titleEditText = findViewById(R.id.f_editable_title).asInstanceOf[EditText]
  lazy val processedImageHandler = new ProcessedImageHandler(Looper.getMainLooper)
  lazy val intentHandler = new IntentHandler(Looper.getMainLooper)
  val imInfoList = ListBuffer[ImageInfo]()

  lazy val mOnThumbClicked =
    (thumbnail: View, info: ImageInfo) =>
      Background {
        val msg = processedImageHandler.obtainMessage
        msg.obj = (imInfoList.indexOf(info), prepareViewerBmps)
        processedImageHandler.sendMessage(msg)
      }

  lazy val mOnThumbLongClicked = (thumbnail: View, info: ImageInfo) => {
    val pop = new PopupMenu(NoteEditActivity.this, thumbnail)
    pop.inflate(R.menu.menu_popup_rem_image)
    pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener {
      override def onMenuItemClick(item: MenuItem): Boolean = {
        item.getItemId match {
          case R.id.popup_warning_remove_image =>
            imageContainer.removeView(thumbnail)
            info.dispose()
            isImagesChanged = true
            if (!imagesAttached) hideImages()
            true
        }
      }
    })
    pop.show()
  }

  def initImInfo(): Unit = {
    if (isNewNote) return
    val note = App.Cache_Floating.note
    if (TextUtils.isEmpty(note.thumbnailNames)) return
    zipEach(note.thumbnailNames.split(','), note.viewerImageNames.split(',')) { (thumb, orig) =>
      val info = new ImageInfo(orig, thumb, newThumbnailView, false, mOnThumbClicked, mOnThumbLongClicked)
      val f = new File(Environment.getExternalStorageDirectory, App.NAME + "/image/" + thumb)
      Picasso.`with`(App.getInstance).load(f).fit.into(info.imageView)
      imageContainer.addView(info.imageView, 0)
      imInfoList.insert(0, info)
    }
  }

  override def contentResId = R.layout.activity_note_edit
  override def serveActivityInstance = this
  override def init() = {
    initSoftwareKeyboardListener()
    initCounter()
    initImInfo()

    if (!isNewNote) {
      val note= App.Cache_Floating.note
      titleEditText.setText(note.title)
      mainEditText.setText(note.content)
      showImages()
    } else {
      setInputMethodVisible()
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.compose_note, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.f_share =>
        val content = mainEditText.getText.toString
        val title = titleEditText.getText.toString
        val sb = new StringBuilder
        if (title != null && !TextUtils.isEmpty(title)) {
          sb.append(title)
          sb.append(" - \n")
        }
        sb.append(content)
        try {
          val in = new Intent
          in.setAction(Intent.ACTION_SEND)
          in.setType("text/plain")
          in.putExtra(Intent.EXTRA_TEXT, sb.toString())
          startActivity(in)
        } catch {
          case e: Exception => e.printStackTrace()
        }
        true

      case R.id.f_attach_image =>
        if (imInfoList.count(!_.isRemoved) >= 3) {
          App.toastShort("Too many photos!")
          return true
        }
        val listItems = Seq(
          new ListItem(getText(R.string.popup_capture_image).toString) {
            def execute() {
              val captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
              mCurrCaptureFilename = App.makeUUIDString + ".jpg"
              val f = IOUtil.makeImageFile(mCurrCaptureFilename)
              captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f))
              startActivityForResult(captureIntent, NoteEditActivity.REQUEST_CAPTURE_IMAGE)
            }
          },
          new ListItem(getText(R.string.popup_select_image).toString) {
            def execute() {
              if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                val in = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                in.addCategory(Intent.CATEGORY_OPENABLE)
                in.setType("image/*")
                startActivityForResult(Intent.createChooser(in, "Select Picture"),
                  NoteEditActivity.REQUEST_SELECT_IMAGE_KK)
              } else {
                val in = new Intent(Intent.ACTION_PICK)
                in.setAction(Intent.ACTION_GET_CONTENT)
                in.setType("image/*")
                startActivityForResult(Intent.createChooser(in, "Select Picture"),
                  NoteEditActivity.REQUEST_SELECT_IMAGE)
              }
            }
          }
        )
        ListDialog.newInstance(None, listItems, None).show(getSupportFragmentManager, "select_image")
        true

      case _ => super.onOptionsItemSelected(item)
    }
  }

  def showImages() = horizontal.setVisibility(View.VISIBLE)
  def hideImages() = horizontal.setVisibility(View.GONE)

  def imagesAttached = imInfoList.exists(!_.isRemoved)
  def noteBodyEmpty = TextUtils.isEmpty(mainEditText.getText.toString.trim)
  def isTitleEmpty = TextUtils.isEmpty(titleEditText.getText.toString.trim)

  private def initSoftwareKeyboardListener() {
    findViewById(R.id.linearLayout_ex).asInstanceOf[LinearLayoutEx].setOnSoftKeyShownListener(
      new LinearLayoutEx.OnSoftKeyShownListener() {
      override def onSoftKeyShown(isShown: Boolean) {
        if (imagesAttached)
          if (isShown) hideImages() else showImages()
      }
    })
  }

  def initCounter() {
    val tv = findViewById(R.id.f_count).asInstanceOf[TextView]
    tv.setText("" + mainEditText.getText.length)
    mainEditText.addTextChangedListener(new TextWatcher {
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
      def afterTextChanged(s: Editable) { }
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) =
        tv.setText(Integer.toString(mainEditText.getText.length))
    })
    tv.setText(Integer.toString(mainEditText.getText.length))
  }

  override def serveNotes2Edam: Option[Seq[NoteRealm]] = {
    if (!noteBodyEmpty || imagesAttached) Some(Seq(createNote))
    else None
  }

  private def createNote = {
    val note =
      if (isNewNote) NoteStore.newNoteObj
      else App.Cache_Floating.note

    // images
    val (orig, thumb) = imInfoList.filterNot(_.isRemoved).foldRight(List(""), List("")) {
      case (info, (origName, thumbName)) => (info.origImName :: origName , info.thumbnailName :: thumbName)
    }
    if (orig.isEmpty) {
      note.thumbnailNames = null
      note.viewerImageNames = null
      note.primaryThumbnailName = null
    } else {
      note.thumbnailNames = thumb.mkString(",")
      note.viewerImageNames = orig.mkString(",")
      note.primaryThumbnailName = thumb.head
    }

    // date
    curDate foreach { case (y, m, d) =>
      note.alarmTime =  y + "/" + (m + 1) + "/" + d
    }

    // others
    note.content = mainEditText.getText.toString.trim
    note.title = titleEditText.getText.toString.trim
    note.parentId = mSelectedPageId
    val now = System.currentTimeMillis
    note.last_modified = now
    if (isNewNote) note.id = now
    note
  }

  override def backPressedCallback(): Unit = {
    hideKeyboard()
    if (!imagesAttached && noteBodyEmpty && isTitleEmpty) {
      if (isNewNote) {
        App.toastShort(R.string.toast_not_saved_new_note)
        finish()
        return
      } else {
        showOnEmptyConfirmDialog()
        return
      }
    }

    Background
    {
      val realm = Realm.getInstance(App.getInstance)
      realm.beginTransaction()
      val note = createNote
      if (!TextUtils.isEmpty(note.thumbnailNames)) {
        ImageUtil.saveBmpPair2external(imInfoList)
      }
      realm.commitTransaction()
      curDate foreach { date =>
        AlarmUtil.set(note.id.toInt, date._1, date._2, date._3)
        App.toastShortInUI(R.string.alarm_setted)
      }
    }

    if (belongChanged) prepareActivityResult()
    finish()
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
    super.onActivityResult(requestCode, resultCode, intent)

    requestCode match {
      case EvernoteSession.REQUEST_CODE_OAUTH =>
        if (resultCode == Activity.RESULT_OK)
          if (EdamService.isLoggedIn) App.toastShort(R.string.toast_edam_auth_success)

      case NoteEditActivity.REQUEST_SELECT_IMAGE |
           NoteEditActivity.REQUEST_CAPTURE_IMAGE |
           NoteEditActivity.REQUEST_SELECT_IMAGE_KK =>
        if (resultCode == Activity.RESULT_OK) {
          isImagesChanged = true
          
          Background
          {
            // get Uri
            val uri = if (requestCode == NoteEditActivity.REQUEST_CAPTURE_IMAGE) {
              Uri.fromFile(new File(Environment.getExternalStorageDirectory + "/" + App.NAME + "/image/", mCurrCaptureFilename))
            } else {
              intent.getData
            }
            val orig = ImageUtil.getBmpByUri(uri)
            val thumb = ImageUtil.newThumbnail(orig, NoteEditActivity.this)
            val msg = intentHandler.obtainMessage
            msg.obj = (orig, thumb)
            intentHandler.sendMessage(msg)
          }
        }
    }
  }

  def newThumbnailView: ImageView = {
    val iv = LayoutInflater.from(this).inflate(R.layout.view_image_preview, null, false).asInstanceOf[ImageView]
    val size = (156 * App.getDisplayScale(this) + 0.5f).toInt
    iv.setLayoutParams(new ViewGroup.LayoutParams(size, size))
    iv
  }

  def prepareViewerBmps: Array[Bitmap] =
    imInfoList.foldLeft(Array[Bitmap]()){ (acc, info) => acc ++ Array(info.getOrigBmp) }

  /** gen new ImageInfo by new Bitmap */
  final class IntentHandler(looper: Looper) extends Handler(looper) {
    override def handleMessage(msg: Message) {
      val (orig: Bitmap, thumb: Bitmap) = msg.obj
      val iv = newThumbnailView
      imageContainer.addView(iv)
      iv.setImageBitmap(thumb)
      iv.setVisibility(View.VISIBLE)

      val newInfo = ImageInfo(
        UUID.randomUUID + ".jpg", 
        UUID.randomUUID + ".jpg", 
        iv,
        isNew=true,
        mOnThumbClicked,
        mOnThumbLongClicked
      )
      
      newInfo.setOrigBmp(orig)
      newInfo.setThumbnailBmp(thumb)
      imInfoList.append(newInfo)
      showImages()
    }
  }

  /** obtain Bitmap from worker thread and then show Viewer */
  final class ProcessedImageHandler(looper: Looper) extends Handler(looper) {
    override def handleMessage(msg: Message) {
      val (pos: Int, bmpArr: Array[Bitmap]) = msg.obj
      new Handler(looper).post(new Runnable() {
        def run() = ViewerDialog.newInstance(bmpArr, pos).show(getFragmentManager, "viewer")
      })
    }
  }
}