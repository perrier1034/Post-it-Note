package com.android.perrier1034.post_it_note.ui

import android.support.v4.app.NavUtils
import android.support.v7.widget.{RecyclerView, StaggeredGridLayoutManager}
import android.view.{ActionMode, Menu, MenuItem, View}
import android.widget.{PopupMenu, TextView, Toast}
import com.android.perrier1034.post_it_note.db.dao.NoteStore
import com.android.perrier1034.post_it_note.model.NoteRealm
import com.android.perrier1034.post_it_note.ui.adapters.NoteAdapter
import com.android.perrier1034.post_it_note.ui.dialog.ConfirmDialog
import com.android.perrier1034.post_it_note.ui.dialog.ConfirmDialog.ClickListener
import com.android.perrier1034.post_it_note.{App, R}
import io.realm.RealmChangeListener
import scala.collection.JavaConversions._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class RubbishActivity extends BaseActivity {
  var mMode: ActionMode = null
  lazy val placeHolder = findViewById(R.id.text_view_when_empty).asInstanceOf[TextView]
  lazy val actionModeCallback = new ActionModeCallbackImpl
  val adapter = new NoteAdapter(None, this,
    // on long tap
    longTap = Some((note: NoteRealm, pos: Int, v: View, count: Int, isActionMode: Boolean, na: NoteAdapter) =>
      if (!isActionMode) getToolbar.startActionMode(actionModeCallback)
    ),
    // on tap
    tap = Some((note: NoteRealm, pos: Int, v: View, count: Int, isActionMode: Boolean, na: NoteAdapter) => {
      if (isActionMode) mMode.setTitle("" + count)
      else {
        val pop = new PopupMenu(this, v)
        pop.inflate(R.menu.menu_popup_rubbish)
        pop.show()
        pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
          def onMenuItemClick(item: MenuItem): Boolean = {
            item.getItemId match {
              case R.id.popup_delete =>
                ConfirmDialog.newInstance(None, getText(R.string.warning_delete_page_completely).toString, None,
                  new ClickListener() {
                    def onClick() = {
                      NoteStore.deleteRealm(note)
                      na.items.get.remove(pos)
                      na.notifyItemRemoved(pos)
                      togglePlaceHolder()
                    }
                  }).show(getFragmentManager, "ConfirmDialog")
              case R.id.popup_resurrection =>
                ConfirmDialog.newInstance(None, getText(R.string.warning_resurrect_page).toString, None,
                  new ClickListener() {
                    def onClick() = {
                      NoteStore.resurrectFromRubbishRealm(note)
                      na.items.get.remove(pos)
                      na.notifyItemRemoved(pos)
                      togglePlaceHolder()
                    }
                  }).show(getFragmentManager, "ConfirmDialog1")
            }
            true
          }
        })
      }
    })
  )

  override def init() = {
    val res = NoteStore.getInitialRubbishNotesRealmAsync
    res.addChangeListener(new RealmChangeListener() {
      def onChange() = {
        adapter.setItems(Some(res.toBuffer.asInstanceOf[ArrayBuffer[NoteRealm]]))
        adapter.notifyDataSetChanged()
        togglePlaceHolder()
        res.removeChangeListeners()
      }
    })
//    Future { NoteStore.getInitialRubbishNotesRealmAsync } onComplete {
//      case Success(notes) =>
//        adapter.setItems(Some(notes.asInstanceOf[ArrayBuffer[NoteRealm]]))
//        adapter.notifyDataSetChanged()
//        togglePlaceHolder()
//      case Failure(e) =>
//        App.toastShort("read error! Please reboot this app")
//        App.L(e.getMessage)
//        togglePlaceHolder()
//    }
    initViews()
  }

  override def contentsResId = R.layout.activity_component_rubbish

  def initViews() {
    val rv = findViewById(R.id.rubbish_recycler_view).asInstanceOf[RecyclerView]
    rv.setAdapter(adapter)
    rv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL))
  }

  def togglePlaceHolder() {
    if (adapter.getItemCount == 0) {
      placeHolder.setVisibility(View.VISIBLE)
      placeHolder.setText("Empty")
    } else {
      placeHolder.setVisibility(View.GONE)
    }
  }

  override def onCreateOptionsMenu(menu: Menu) = true

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        NavUtils.navigateUpFromSameTask(this)
        return true

      case R.id.clean_rubbish =>
        ConfirmDialog.newInstance(None, getText(R.string.cdialog_warning_clear_rubbish).toString, None,
          new ClickListener() {
            def onClick() {
              NoteStore.cleanRubbishRealm()
              adapter.setItems(None)
              adapter.notifyDataSetChanged()
              togglePlaceHolder()
              Toast.makeText(App.getInstance, getText(R.string.clean_rubbish_toast), Toast.LENGTH_SHORT).show()
            }
          }).show(getFragmentManager, "ConfirmDialog1")
        return true
    }
    super.onOptionsItemSelected(item)
  }

  final class ActionModeCallbackImpl extends ActionMode.Callback {
    def onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = {
      mMode = mode
      mode.setTitle("1")
      getMenuInflater.inflate(R.menu.action_mode_rubbish, menu)
      true
    }

    def onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = {
      setActionModeStatusBarColorLOLLIPOP()
      true
    }

    def onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = {
      def cutSelectedItemsFromAdapter(notes: Seq[NoteRealm]) = {
        val items = adapter.items.get
        adapter.setItems(Some(items.diff(Seq(notes))))
      }
      item.getItemId match {
        case R.id.action_destroy_notes =>
          ConfirmDialog.newInstance(None, getText(R.string.cdialog_warning_destroy_selected_notes).toString, None,
            new ClickListener() { def onClick() = {
              adapter.getSelectedItems match {
                case Some(notes) =>
                  NoteStore.destroySelectedNotesRealm(notes)
                  cutSelectedItemsFromAdapter(notes)
                  App.toastShort(R.string.wipe_selected_notes_toast)
                  mode.finish()
                  adapter.items = Some(adapter.items.get diff notes)
                  adapter.notifyDataSetChanged()
                  togglePlaceHolder()
                case None => App.toastShort("no items are selected")
              }
            }}).show(getFragmentManager, "ConfirmDialog2")

        case R.id.action_resurrect_notes =>
          ConfirmDialog.newInstance(None, getText(R.string.warning_resurrect_page_multi_select).toString, None,
            new ClickListener() { def onClick() {
              adapter.getSelectedItems match {
                case Some(notes) =>
                  NoteStore.resurrectRealm(notes)
                  cutSelectedItemsFromAdapter(notes)
                  App.toastShort(R.string.ressurect_selected_notes_toast)
                  mode.finish()
                  adapter.items = Some(adapter.items.get diff notes)
                  adapter.notifyDataSetChanged()
                  togglePlaceHolder()
                case None => App.toastShort("no items are selected")
              }
            }}).show(getFragmentManager, "ConfirmDialog3")

        case R.id.action_select_all =>
          if (adapter.getSelectedItemCount < adapter.getItemCount) {
            adapter.selectAll()
            mMode.setTitle("" + adapter.getItemCount)
          }
          else {
            adapter.unSelectAll()
            mode.setTitle("0")
          }
      }
      true
    }

    def onDestroyActionMode(mode: ActionMode) {
      adapter.stopActionMode()
      mMode = null
    }
  }

}