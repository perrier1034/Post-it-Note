package com.android.perrier1034.post_it_note.ui

import android.app.Activity

import android.support.design.widget.Snackbar
import android.view.View.OnFocusChangeListener
import com.android.perrier1034.post_it_note.model.NoteRealm
import com.android.perrier1034.post_it_note.ui.PageManager.Callback2AttachedFragments
import io.realm.{Sort, RealmResults, RealmChangeListener}

import scala.collection.JavaConversions._
import android.os.Bundle
import android.support.v4.content.Loader
import android.support.v4.app.{LoaderManager, Fragment}
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView.{OnCloseListener, OnQueryTextListener}
import android.support.v7.widget.{RecyclerView, SearchView, StaggeredGridLayoutManager}
import android.text.TextUtils
import android.view.{ActionMode, LayoutInflater, Menu, MenuInflater, MenuItem, View, ViewGroup}
import android.widget.TextView
import com.android.perrier1034.post_it_note.db.dao.NoteStore
import com.android.perrier1034.post_it_note.ui.adapters.NoteAdapter
import com.android.perrier1034.post_it_note.ui.dialog.{ListItem, ListDialog}
import com.android.perrier1034.post_it_note.{Constants, EdamService, App, R}

import scala.collection.mutable.ArrayBuffer

object PageFragment {

  val KEY_MODEL_ID = "ID"

  def newInstance(id: Long): PageFragment = {
    val instance = new PageFragment
    val args = new Bundle
    args.putInt(KEY_MODEL_ID, id.toInt)
    instance.setArguments(args)
    instance
  }

  trait Callback2Activity {
    def onNoteClicked(id: Int, pageId: Int, cls: Class[_])
    def onStartActionMode(menu: Menu)
    def onActionModeFinished(pos: Int)
    def onFilteringStarted()
    def onFilteringFinished()
  }

}

// A little stateful class because of Fuckin' Fragment
final class PageFragment extends Fragment with Callback2AttachedFragments {

  lazy val mActionModeCallback = new ActionModeCallbackImpl

  var mCallback2Activity: PageFragment.Callback2Activity = null

  var mMode: ActionMode = null

  var resultAsync: RealmResults[NoteRealm] = null

  val mAdapter = new NoteAdapter(None, App.getInstance,
    tap = Some((note: NoteRealm, pos: Int, v: View, count: Int, isActionMode: Boolean, nd: NoteAdapter) => {
      if (isActionMode)
        mMode.setTitle("" + count)
      else {
        App.Cache_Floating.note = note
        val cls = if (TextUtils.isEmpty(note.checkItems)) classOf[NoteEditActivity]
                  else classOf[CheckListActivity]
        mCallback2Activity.onNoteClicked(note.id.toInt, note.parentId, cls)
      }
    }),
    longTap = Some((note: NoteRealm, pos: Int, v: View, count: Int, isActionMode: Boolean, nd: NoteAdapter) => {
      if (!isActionMode)
        getActivity.findViewById(R.id.toolbar).startActionMode(mActionModeCallback)
    })
  )

  override def onAttach(ctx: Activity) {
    super.onAttach(ctx)
    mCallback2Activity = ctx.asInstanceOf[PageManager].getCallback2Activity
  }

  // callback to activity
  override def onNoteOrderChanged() = {
    val order = App.getInstance.noteOrderRealm
    resultAsync.sort(order, Sort.DESCENDING)
    mAdapter.setItems(Some(resultAsync.toBuffer.asInstanceOf[ArrayBuffer[NoteRealm]]))
    mAdapter.notifyDataSetChanged()
    togglePlaceHolder()
  }

  // about filtering  --------------------------------------------------------------------
  var beforeFiltering: Option[ArrayBuffer[NoteRealm]] = None
  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

    inflater.inflate(R.menu.main, menu)
    val searchMenuItem = menu.findItem(R.id.menu_search)
    val sv = MenuItemCompat.getActionView(searchMenuItem).asInstanceOf[SearchView]

    lazy val queryTextListener = new OnQueryTextListener {
      def onQueryTextSubmit(searchWord: String): Boolean = true
      def onQueryTextChange(newText: String): Boolean = {
        mAdapter.setItems(beforeFiltering match {
          case Some(items) =>
            // actual filtering task
            Some(items.filter{ note =>
              if (!TextUtils.isEmpty(note.checkItems)) note.checkItems.contains(newText)
              else note.content.contains(newText) || note.title.contains(newText)
            })
          case None => None
        })
        mAdapter.notifyDataSetChanged()
        true
      }
    }

    val closeListener = new OnCloseListener {
      def onClose: Boolean = {
        mAdapter.setItems(beforeFiltering)
        beforeFiltering = None
        mAdapter.notifyDataSetChanged()
        mCallback2Activity.onFilteringFinished()
        false
      }
    }

    lazy val searchClickListener = new View.OnClickListener {
      def onClick(v: View) = {
        beforeFiltering = mAdapter.items
        mCallback2Activity.onFilteringStarted()
      }
    }

    lazy val focusListener = new OnFocusChangeListener() {
      override def onFocusChange(v: View, hasFocus: Boolean) {
        // Ugly hack!
        // Without 1st `if`, onClose() will called twice badly.
        // 1. When SearchTextView lose focus
        // 2. When usual close-button-clicked-listener
        if (beforeFiltering.isDefined)
          if (!hasFocus) callOnClose()
      }
    }

    def callOnClose() = sv.setIconified(true)

    sv.setQueryHint(getText(R.string.filtering))
    sv.setIconifiedByDefault(true)
    sv.setOnQueryTextListener(queryTextListener)
    sv.setOnCloseListener(closeListener)
    sv.setOnSearchClickListener(searchClickListener)
    sv.setOnQueryTextFocusChangeListener(focusListener)
    super.onCreateOptionsMenu(menu, inflater)
  }
  // about filtering end ------------------------------------------------------------------

  // used only for SnackBar
  var root: ViewGroup = null
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    getActivity.asInstanceOf[PageManager].addListenerFragment(getPageId, this)
    root = inflater.inflate(R.layout.fragment_page, container, false).asInstanceOf[ViewGroup]
    initChildViews(root)
    setHasOptionsMenu(true)

    resultAsync = NoteStore.notesByPageIdRealmAsync(getPageId, App.getInstance.noteOrderRealm)
    resultAsync.addChangeListener(
      new RealmChangeListener() {
        def onChange() {
          mAdapter.setItems(Some(resultAsync.toBuffer.asInstanceOf[ArrayBuffer[NoteRealm]]))
          mAdapter.notifyDataSetChanged()
          togglePlaceHolder()
        }
      }
    )

    root
  }

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
    getActivity.supportInvalidateOptionsMenu()
  }

  var mPlaceHolder: TextView = null
  def togglePlaceHolder() =
    if (mAdapter.getItemCount == 0) {
      mPlaceHolder.setVisibility(View.VISIBLE)
      mPlaceHolder.setText("Empty")
    } else {
      mPlaceHolder.setVisibility(View.GONE)
    }

  def initChildViews(root: ViewGroup) = {
    mPlaceHolder = root.findViewById(R.id.text_view_when_empty).asInstanceOf[TextView]
    val rv = root.findViewById(R.id.main_list).asInstanceOf[RecyclerView]
    rv.setAdapter(mAdapter)
    rv.setLayoutManager(new StaggeredGridLayoutManager(if (App.getInstance.isTablet(getActivity)) 3 else 2,
    StaggeredGridLayoutManager.VERTICAL))
  }

  def getPage = App.Cache_Floating.pages.filter(_.id == getPageId).head
  def getPageId = getArguments.getInt(PageFragment.KEY_MODEL_ID)

  override def onDestroyView() {
    getActivity.asInstanceOf[PageManager].removeListenerFragment(getPageId)
    resultAsync.removeChangeListeners()
    setMenuVisibility(false)
    super.onDestroyView()
  }

  final class ActionModeCallbackImpl extends ActionMode.Callback {

    var pos = 0

    override def onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = {
      mMode = mode
      mode.setTitle("1")
      mCallback2Activity.onStartActionMode(menu)
      pos = -1
      if (!EdamService.isLoggedIn) menu.findItem(R.id.action_export_evernote).setVisible(false)
      true
    }

    override def onPrepareActionMode(mode: ActionMode, menu: Menu) = true

    override def onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = {
      item.getItemId match {
        case R.id.action_delete_notes =>
          mAdapter.getSelectedItems match {
            case Some(notes) =>
              NoteStore.moveToRubbishRealm(notes)
              Snackbar.make(root, R.string.snack_log_deleted_note_selected , Snackbar.LENGTH_SHORT)
                .setAction(R.string.snack_button_resurrection, new View.OnClickListener() {
                def onClick(v: View ) = NoteStore.resurrectRealm(notes)
              }).show()
              mAdapter.notifyDataSetChanged()
              mode.finish()
            case None => App.toastShortInUI("No items are selected")
          }

        case R.id.action_select_all =>
          if (mAdapter.getSelectedItemCount < mAdapter.getItemCount) {
            mAdapter.selectAll()
            mode.setTitle("" + mAdapter.getItemCount)
          } else {
            mAdapter.unSelectAll()
            mode.setTitle("0")
          }

        case R.id.ic_action_notes_transfer =>
          mAdapter.getSelectedItems match {
            case Some(notes) => showTransferDialog(mode, notes)
            case None => App.toastShort("No items are selected")
          }

        case R.id.action_export_evernote =>
          mAdapter.getSelectedItems match {
            case Some(notes) =>
              EdamService.saveNotesRealm(notes, getFragmentManager, Some(() => mode.finish()))
            case None => App.toastShort("No items are selected")
          }
      }
      true
    }

    private def showTransferDialog(mode: ActionMode, notes: Seq[NoteRealm]) {
      val pages = App.Cache_Floating.pages
      val listItems = pages map { page =>
        new ListItem(page.title) {
          override def execute() = {
            NoteStore.transferRealm(notes, page.id.toInt)
            pos = pages.indexOf(page)
            mode.finish()
          }
        }
      }
      ListDialog.newInstance(Some(getText(R.string.ldialog_transfer).toString), listItems, None)
        .show(getFragmentManager, "showTransferDialog")
    }

    def onDestroyActionMode(mode: ActionMode) {
      mAdapter.stopActionMode()
      mCallback2Activity.onActionModeFinished(pos)
      mMode = null
    }
  }

}