package com.android.perrier1034.post_it_note.ui.adapters

import java.io.File
import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import android.content.Context
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.{ImageView, TextView}
import com.android.perrier1034.post_it_note.db.dao.NoteStore
import com.android.perrier1034.post_it_note.model.NoteRealm
import com.android.perrier1034.post_it_note.ui.{MultiSelectionSupport => MSS}
import com.android.perrier1034.post_it_note.{Constants, App, R}
import com.android.perrier1034.post_it_note.ui.adapters.NoteAdapter.{VH, cb}
import com.squareup.picasso.Picasso

import scala.collection.mutable.ArrayBuffer

object NoteAdapter {
  // (note, pos, view, count, isActionMode, adapter)
  type cb = Option[(NoteRealm, Int, View, Int, Boolean, NoteAdapter) => Unit]
  val sDateFormat = new SimpleDateFormat("y/M/d", Locale.US)
  case class VH(rowView: View,
                container: View,
                titleView: TextView,
                bodyView: TextView,
                dateView: TextView,
                thumbnail: ImageView,
                checkItems: Array[TextView]) extends RecyclerView.ViewHolder(rowView)

}

final class NoteAdapter(var items: Option[ArrayBuffer[NoteRealm]], ctx: Context, tap: cb, longTap: cb)
  extends RecyclerView.Adapter[VH] {

  var mRecyclerView: RecyclerView = null

  lazy val mss = new MSS(new MSS.Client {
    override def onActionModeStopped() = notifyDataSetChanged()
    override def onSelectionChanged() =  notifyDataSetChanged()
  })

  lazy val longClickListener = new View.OnLongClickListener {
    def onLongClick(v: View): Boolean = {
      val pos = mRecyclerView.getChildAdapterPosition(v)
      longTap.foreach(_(getItem(pos), pos, v, getItemCount, mss.isActionMode, NoteAdapter.this))
      if (!mss.isActionMode) {
        mss.startActionMode()
        mss.toggleSelection(mRecyclerView.getChildAdapterPosition(v))
      }
      true
    }}

  val clickListener = new View.OnClickListener {
    def onClick(v: View) {
      val pos = mRecyclerView.getChildAdapterPosition(v)
      if (mss.isActionMode) mss.toggleSelection(pos)
      val count = mss.getSelectedItemCount
      v.setTag(getItemId(pos))
      tap foreach { _(getItem(pos), pos, v, count, mss.isActionMode, NoteAdapter.this) }
    }}

  setHasStableIds(true)

  override def getItemCount = items match {
    case Some(notes) => notes.size
    case None => 0
  }

  def getItem(pos: Int): NoteRealm = items match {
    case Some(notes) => notes(pos)
    case None => null
  }

  override def getItemId(position: Int) = getItem(position).id

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = {

    mRecyclerView = parent.asInstanceOf[RecyclerView]

    val rowView = LayoutInflater.from(parent.getContext).inflate(R.layout.row_main_note, parent, false)
    val container = rowView.findViewById(R.id.container).asInstanceOf[ViewGroup]
    val titleView = rowView.findViewById(R.id.title).asInstanceOf[TextView]
    val dateView = rowView.findViewById(R.id.picked).asInstanceOf[TextView]
    val thumbnail = rowView.findViewById(R.id.thumbnail).asInstanceOf[ImageView]
    val checkItemContainer = container.getChildAt(1).asInstanceOf[ViewGroup]
    val bodyView = checkItemContainer.getChildAt(0).asInstanceOf[TextView]
    val checkItems = Array(
      checkItemContainer.getChildAt(1).asInstanceOf[TextView],
      checkItemContainer.getChildAt(2).asInstanceOf[TextView],
      checkItemContainer.getChildAt(3).asInstanceOf[TextView],
      checkItemContainer.getChildAt(4).asInstanceOf[TextView],
      checkItemContainer.getChildAt(5).asInstanceOf[TextView]
    )
    rowView.setOnClickListener(clickListener)
    rowView.setOnLongClickListener(longClickListener)
    VH(rowView, container, titleView, bodyView, dateView, thumbnail, checkItems)
  }

  override def onBindViewHolder(vh: VH, position: Int) = {
    val note = getItem(position)
    val isCheckList = !TextUtils.isEmpty(note.checkItems)
    val hasTitle = !TextUtils.isEmpty(note.title)
    // title
    if (hasTitle) {
      vh.titleView.setVisibility(View.VISIBLE)
      vh.titleView.setText(note.title)
    } else {
      vh.titleView.setVisibility(View.GONE)
    }

    // image
    // !isCheckList is needed because thumbnail appear when
    // view was re-used even if isCheckList
    if (!TextUtils.isEmpty(note.primaryThumbnailName) && !isCheckList) {
      if (hasTitle) vh.titleView.setBackgroundColor(0x99ffffff)
      val f = new File(App.imageDirName + note.primaryThumbnailName)
      (Picasso`with` ctx).load(Uri.fromFile(f)).fit.into(vh.thumbnail)
      vh.thumbnail.setVisibility(View.VISIBLE)
    } else {
      vh.thumbnail.setVisibility(View.GONE)
      if (hasTitle) vh.titleView.setBackgroundColor(0x00000000)
    }

    // body
    if (vh.thumbnail.getVisibility == View.VISIBLE && !isCheckList)
      vh.bodyView.setMaxLines(1) // If I have image, set singleLine
    else
      vh.bodyView.setMaxLines(5) // no image

    // create check list
    if (isCheckList) {
      //vh.thumbnail.setVisibility(View.GONE)
      vh.bodyView.setVisibility(View.GONE)
      vh.checkItems foreach { _.setVisibility(View.GONE) }
      (note.checkItems.split(','), note.checkStates, vh.checkItems).zipped foreach {
        case (text, state, view) =>
          val isChecked = '1' == state
          view.setVisibility(View.VISIBLE)
          view.setText(text)
          if (isChecked) view.setTextColor(0xcccccccc)
          else view.setTextColor(0xff696969)
      }
    } else {
      vh.checkItems foreach { _.setVisibility(View.GONE) }
      if (TextUtils.isEmpty(note.content))
        vh.bodyView.setVisibility(View.GONE)
      else {
        vh.bodyView.setVisibility(View.VISIBLE)
        vh.bodyView.setText(note.content)
      }
    }

    //date
    if (note.alarmTime != null && note.inRubbish == 0) { // show alarm
      vh.dateView.setText(note.alarmTime)
      vh.dateView.setTextColor(Constants.TEXT_COLOR_NOTE_ALARM)
      vh.dateView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_alarm, 0, 0, 0)
    } else { // show last modified time
      val d = new Date(note.last_modified.toLong)
      vh.dateView.setText(NoteAdapter.sDateFormat.format(d))
      vh.dateView.setTextColor(Constants.TEXT_COLOR_DATE_DEFAULT)
      vh.dateView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }

    // selection
    if (mss.isNotActionMode) vh.container.setActivated(false)
    else vh.container.setActivated(mss.isSelected(position))
  }

  override def getItemViewType(pos: Int) = 0

  def curSelectedItems: Seq[NoteRealm] = mss.getSelectedItemPositions.indices.map(getItem)

  def getSelectedItems: (Option[Seq[NoteRealm]]) = {
    val items = curSelectedItems
    if (items.isEmpty) None else Some(items)
  }

  def setItems(data: Option[ArrayBuffer[NoteRealm]]): Option[Seq[NoteRealm]] = {
    val a = items
    items = data
    a
  }

  def destroySelectedItems() = NoteStore.destroySelectedNotesRealm(items.get)

  def startActionMode() = mss.startActionMode()

  def stopActionMode() = mss.stopActionMode()

  def selectAll() = mss.selectAll(getItemCount)

  def unSelectAll() = mss.unSelectAll()

  def getSelectedItemCount = mss.getSelectedItemCount

  def isActionMode = mss.isActionMode

}
