package com.android.perrier1034.post_it_note.ui
import android.graphics.drawable.NinePatchDrawable
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.text.TextUtils
import android.view._
import android.widget._
import com.android.perrier1034.post_it_note._
import com.android.perrier1034.post_it_note.model.{NoteRealm, CheckItem}
import com.android.perrier1034.post_it_note.util.SeqUtil._
import com.android.perrier1034.post_it_note.ui.adapters.CheckListAdapter
import com.android.perrier1034.post_it_note.util.AlarmUtil
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.{BasicSwapTargetTranslationInterpolator, RecyclerViewDragDropManager}
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager
import io.realm.{Realm, RealmObject}

import scala.collection.mutable.ArrayBuffer

object CheckListActivity {
  val INTENT_KEY_COMPOSE_BUNDLE = "INTENT_KEY_COMPOSE_BUNDLE"
  val BUNDLE_KEY_SELECTED_PAGE_ID = "BUNDLE_KEY_SELECTED_PAGE_ID"
  val BUNDLE_KEY_DELETE_CLICKED = "BUNDLE_KEY_DELETE_CLICKED "
  val BUNDLE_KEY_CUR_NOTE_ID = "BUNDLE_KEY_CUR_NOTE_ID"
}

final class CheckListActivity extends BaseFloating {

  lazy val mRecyclerView = findViewById(R.id.recycler_view).asInstanceOf[RecyclerView]
  lazy val mEditTextTitle = findViewById(R.id.f_editable_title).asInstanceOf[EditText]
  lazy val mCheckListAdapter = new CheckListAdapter(
    genCheckListModels.asInstanceOf[ArrayBuffer[CheckItem]], mRecyclerView)
  var addRowClicked = false

  def genCheckListModels: Seq[CheckItem] = {
    if (isNewNote) {
      ArrayBuffer[CheckItem]()
    } else {
      val note = App.Cache_Floating.note
      zipMap(note.checkItems.split(','), note.checkItems) { (text, state) =>
        new CheckItem(App.genUniqueNum, text, '1' == state, false)
      }.toBuffer
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.compose_check, menu)
    super.onCreateOptionsMenu(menu)
  }

  def contentResId = R.layout.activity_check_list
  def serveActivityInstance = this

  override def init() = {
    if (!isNewNote) {
      val note = App.Cache_Floating.note
      mEditTextTitle.setText(note.title)
    }
    initCheckList()
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.f_add_list_item =>
        val count = mCheckListAdapter.getItemCount

        if (count > Constants.MAX_CHECK_ITEM_COUNT)
          App.toastShort("Max item count is 100")
        else
          mCheckListAdapter.appendNewRow(count)
        true
      case _ => super.onOptionsItemSelected(item)
    }
  }

  private def genDestModel = mCheckListAdapter.items.filter{ item => !TextUtils.isEmpty(item.body) }

  private def createNote(models: Seq[CheckItem]): NoteRealm = {
    val realm = Realm.getInstance(App.getInstance)
    val note = if (!isNewNote) App.Cache_Floating.note else realm.createObject(classOf[NoteRealm])
    val (a, b) = models.foldRight(("", "")) { (model, acc) =>
      ( model.body ++: (',' +: acc._1), (if (model.isChecked) '1' else '0') +: acc._2)
    }
    note.checkItems = a
    note.checkStates = b
    note.parentId = mSelectedPageId
    note.title =  mEditTextTitle.getText.toString.trim
    note.last_modified = System.currentTimeMillis
    // date
    curDate foreach { case (y, m, d) => note.alarmTime = y + "/" + (m + 1) + "/" + d }
    note
  }

  override def backPressedCallback(): Unit = {
    hideKeyboard()
    val destModel = genDestModel
    if (destModel.isEmpty) {
      if (isNewNote) {
        App.toastShort(R.string.toast_not_saved_new_note)
        finish()
      } else {
        showOnEmptyConfirmDialog()
      }
      return
    }

    Background
    {

      val realm = Realm.getInstance(App.getInstance)
      realm.beginTransaction()
      val note = createNote(destModel)
      realm.commitTransaction()
      curDate foreach { date =>
        AlarmUtil.set(note.id, date._1, date._2, date._3)
        App.toastShortInUI(R.string.alarm_setted)
      }
    }

    if (belongChanged) prepareActivityResult()
    finish()
  }

  override def serveNotes2Edam: Option[Seq[NoteRealm]] = {
    val models = genDestModel.toSeq
    if (models.nonEmpty) Some(Seq(createNote(models)))
    else None
  }

  /**
   * initialize RecyclerView and its adapter
   */
  private def initCheckList() {
    val tgManager = new RecyclerViewTouchActionGuardManager
    tgManager.setInterceptVerticalScrollingWhileAnimationRunning(true)
    tgManager.setEnabled(true)
    val ddManager = new RecyclerViewDragDropManager
    ddManager.setSwapTargetTranslationInterpolator(new BasicSwapTargetTranslationInterpolator())
    ddManager.setDraggingItemShadowDrawable(getResources.getDrawable(R.drawable.material_shadow_z3_9).asInstanceOf[NinePatchDrawable])
    val swipeManager = new RecyclerViewSwipeManager
    val wrappedAdapter = swipeManager.createWrappedAdapter(ddManager.createWrappedAdapter(mCheckListAdapter))
    val animator = new SwipeDismissItemAnimator
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this))
    mRecyclerView.setAdapter(wrappedAdapter)
    mRecyclerView.setItemAnimator(animator)
    mRecyclerView.addItemDecoration(new DividerAdder(this))
    tgManager.attachRecyclerView(mRecyclerView)
    swipeManager.attachRecyclerView(mRecyclerView)
    ddManager.attachRecyclerView(mRecyclerView)
    if (isNewNote) mCheckListAdapter.appendNewRow(0)
  }

}