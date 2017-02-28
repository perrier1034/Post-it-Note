package com.android.perrier1034.post_it_note.ui

import android.app.Activity

import android.content.{Intent, Context}
import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, Menu, MenuItem, View, ViewGroup}
import android.widget._
import com.android.perrier1034.post_it_note.model.PageRealm
import com.android.perrier1034.post_it_note.ui.dialog.EditTextDialog.ClickListener
import com.android.perrier1034.post_it_note.ui.dialog.{ColorChooserDialog, ConfirmDialog, EditTextDialog}
import com.android.perrier1034.post_it_note.{Background, App, Constants, R}
import io.realm.Realm

// orz
import scala.collection.mutable.ArrayBuffer

object PageSettingActivity {
  val REQUEST_CODE = 0x16
}

class PageSettingActivity extends BaseActivity {

  lazy val mRecyclerView = findViewById(R.id.recycler_view).asInstanceOf[RecyclerView]
  val adapter = new Adapter(App.Cache_PSA.pages.asInstanceOf[ArrayBuffer[PageRealm]])
  var listChanged = false

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.tabs, menu)
    true
  }

  override def init() = {
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this))
    mRecyclerView.setAdapter(adapter)
    val itemDecor = new ItemTouchHelper(
      new SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
        override def isItemViewSwipeEnabled = false
        override def onSwiped(vh: ViewHolder , direction: Int) = {}
        override def onMove(rv: RecyclerView, vh: ViewHolder, target: ViewHolder): Boolean = {
          val from = vh.getAdapterPosition
          val to = target.getAdapterPosition
          adapter.swapItem(from, to)
          listChanged = true
          adapter.notifyItemMoved(from, to)
          true
        }
      })
    itemDecor.attachToRecyclerView(mRecyclerView)
  }

  override def onBackPressed() {
    if (listChanged) {
      setResult(Activity.RESULT_OK, new Intent)

      App.Cache_PSA.pages = adapter.items

      // save in db
      Background {
        val realm = Realm.getInstance(App.getInstance)
        // transaction
        realm.beginTransaction()
        try {
          adapter.items foreach { item =>
            item.pageOrder = adapter.items.indexOf(item)
          }
          adapter.removedItems foreach { useless =>
            try useless.removeFromRealm()
            catch { case e: Exception => /* new note that don't exists in db */ }
          }
          // end transaction
        } catch {
          case e: Exception =>
            App.toastShortInUI("Saving changes failed.")
            e.printStackTrace()
        } finally { realm.commitTransaction() }
      }
      listChanged = false
    }
    super.onBackPressed()
  }

  val editTextListener = new ClickListener { def onClick(str: String) = {
    App.Cache_PSA.curPage match {
      case Some(page) =>
        page.title = str
        App.Cache_PSA.curPage = None
        adapter.items.append(page)
      case None =>

        val page = Realm.getInstance(App.getInstance).createObject(classOf[PageRealm])
        page.title = str
        page.pageColorPos = Constants.DEFAULT_BG_COLOR_POS_BAR
        adapter.items.append(page)
    }
    listChanged = true
    adapter.notifyItemInserted(adapter.getItemCount)
  }}

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        finish()
        return true
      case R.id.add_tab =>
        if (adapter.getItemCount >= Constants.MAX_PAGE_COUNT) {
          App.toastShort(R.string.toast_reaching_max_page_count)
          return true
        }
        EditTextDialog.newInstance(hint=Some("New page"), limit=Some(12), lis=editTextListener)
          .show(getFragmentManager, "editDialog0")
        return true
    }
    super.onOptionsItemSelected(item)
  }

  override def contentsResId = R.layout.activity_component_setting_page
  
  def showConfDialog(page: PageRealm) = {
    ConfirmDialog.newInstance(Some(page.title),
      getText(R.string.cdialog_warning_delete_page).toString, None,
      new ConfirmDialog.ClickListener() { def onClick() {
        adapter.removeItem(page)
        listChanged = true
      }}).show(getFragmentManager, "confDialog")
  }
  
  def showEditDialog(page: PageRealm) = {
    EditTextDialog.newInstance(hint=Some("title"),
      defaultValue=Some(page.title), limit=Some(12), lis=editTextListener)
      .show(getFragmentManager, "editDialog")
  }
  
  def showPopup(page: PageRealm, parent: View) = {
    val pop = new PopupMenu(PageSettingActivity.this, parent)
    pop.inflate(R.menu.menu_popup_page_setting)
    pop.show()
    pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      def onMenuItemClick(item: MenuItem): Boolean = {
        item.getItemId match {
          case R.id.popup_delete => showConfDialog(page)
          case R.id.popup_rename => showEditDialog(page)
        }
        true
      }
    })
  }
  
  def showColorChooseDialog(page: PageRealm, target: View) = {
    new ColorChooserDialog().show(PageSettingActivity.this,
      page.pageColorPos, new ColorChooserDialog.Callback() {
        def onColorSelection(idx: Int, color: Int, darker: Int) {
          page.pageColorPos = idx
          listChanged = true
          val shape: GradientDrawable = target.getBackground.asInstanceOf[GradientDrawable]
          shape.setColor(color)
          shape.setStroke(1, Constants.COLOR_CIRCLE_STROKE)
          target.setBackground(shape)
        }
      })
  }

  final class Adapter(var items: ArrayBuffer[PageRealm]) extends RecyclerView.Adapter[VH] {

    val removedItems = scala.collection.mutable.Set[PageRealm]()
    lazy val mInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

    def getItem(pos: Int) = items(pos)
    def removeItem(item: PageRealm) = {
      val pos = items.indexOf(item)
      removedItems.add(items.remove(pos))
      notifyItemRemoved(pos)
    }

    override def getItemCount = items.size
    override def getItemId(pos: Int) = pos

    def swapItem(from: Int, to: Int) = {
      val item = items.remove(from)
      items.insert(to, item)
    }

    override def onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = {
      val root = mInflater.inflate(R.layout.row_page_setting, parent, false)
      VH (
        root,
        root.findViewById(R.id.relativeLayout).asInstanceOf[ViewGroup],
        root.findViewById(R.id.dd_title).asInstanceOf[TextView]
      )
    }

    override def onBindViewHolder(vh: VH, pos: Int) = {
      val page = getItem(pos)
      val listListener = new OnClickListener { def onClick(view: View) = {
        if (page.id == 1)
          App.toastShort(R.string.toast_warning_page_all_edit)
        else {
          App.Cache_PSA.curPage = Some(page)
          showPopup(page, view)
        }
      }}

      vh.root.setOnClickListener(listListener)

      vh.textView.setText(page.title)
      val circle = vh.colorObj.findViewById(R.id.color_circle)
      val shape = circle.getBackground.asInstanceOf[GradientDrawable]
      shape.setColor(Constants.PAGER_COLOR_MAPPING(getItem(pos).pageColorPos)(0))
      shape.setStroke(1, Constants.COLOR_CIRCLE_STROKE)
      circle.setBackground(shape)
      vh.colorObj.setOnClickListener(new View.OnClickListener {
        def onClick(v: View) = showColorChooseDialog(adapter.getItem(pos), circle)
      })
    }
  }

  case class VH(root: View, colorObj: ViewGroup, textView: TextView)
    extends RecyclerView.ViewHolder(root)

}