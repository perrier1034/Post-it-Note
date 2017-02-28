package com.android.perrier1034.post_it_note.ui.adapters

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.text.{Editable, TextWatcher}
import android.view.View.OnClickListener
import android.view.{KeyEvent, LayoutInflater, MotionEvent, View, ViewGroup}
import android.widget.CheckBox
import com.android.perrier1034.post_it_note.ui.views.HackyEditText
import com.android.perrier1034.post_it_note.{App, R}
import com.android.perrier1034.post_it_note.model.CheckItem
import com.android.perrier1034.post_it_note.ui.adapters.CheckListAdapter.VH
import com.android.perrier1034.post_it_note.util.ViewUtil
import com.h6ah4i.android.widget.advrecyclerview.draggable.{DraggableItemAdapter, RecyclerViewDragDropManager}
import com.h6ah4i.android.widget.advrecyclerview.swipeable.{RecyclerViewSwipeManager, SwipeableItemAdapter}
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder

import scala.collection.mutable.ArrayBuffer

object CheckListAdapter {

  case class VH(root: View,
                container: ViewGroup,
                dragHandle: View,
                editText: HackyEditText,
                checkBox: CheckBox) extends AbstractDraggableSwipeableItemViewHolder(root) {

    override def getSwipeableContainerView: View = container
  }

}

final class CheckListAdapter (var items: ArrayBuffer[CheckItem], recyclerView: RecyclerView)
  extends RecyclerView.Adapter[VH]
  with DraggableItemAdapter[VH]
  with SwipeableItemAdapter[VH] {

  private var newRowPos = 0
  private var shouldRequestFocus = false

  setHasStableIds(true)

  val mOnClickListenerCheckBox = new OnClickListener {
    override def onClick(v: View) {
      val pos = v.getTag.asInstanceOf[Int]
      items(pos).toggleCheckState()
      notifyItemChanged(pos)
    }
  }

  val mOnEnterClickedListener = new View.OnKeyListener() {
    def onKey(v: View, keyCode: Int, event: KeyEvent): Boolean = {
      if (keyCode == KeyEvent.KEYCODE_ENTER) {
        if (event.getAction == MotionEvent.ACTION_DOWN) {
          val pos = v.getTag.asInstanceOf[Int]
          if (pos == getItemCount - 1) {
            // add
            items.append(new CheckItem(App.genUniqueNum, body=null, isChecked=false, false))
            val newLen = getItemCount
            notifyItemInserted(newLen - 1)
            recyclerView.getLayoutManager.scrollToPosition(newLen - 1)
            newRowPos = newLen - 1
          } else {
            // insert
            items.insert(pos + 1, new CheckItem(App.genUniqueNum, body=null, isChecked=false, false))
            notifyItemInserted(pos + 1)
            recyclerView.getLayoutManager.scrollToPosition(pos + 1)
            newRowPos = pos + 1
          }
          shouldRequestFocus = true
          return true
        }
      }
      false
    }
  }

  override def getItemId(pos: Int): Long = items(pos).id

  override def getItemViewType(position: Int): Int = 0

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = {
    val root = LayoutInflater.from(parent.getContext).inflate(R.layout.row_check, parent, false)
    val container  = root.findViewById(R.id.container).asInstanceOf[ViewGroup]
    val dragHandle = root.findViewById(R.id.check_list_handle)
    val editText   = root.findViewById(R.id.main_text_row_note).asInstanceOf[HackyEditText]
    val checkBox   = root.findViewById(R.id.checkbox_check_list).asInstanceOf[CheckBox]

    val vh = new VH(root, container, dragHandle, editText, checkBox)
    checkBox.setOnClickListener(mOnClickListenerCheckBox)
    editText.setOnKeyListener(mOnEnterClickedListener)
    editText.addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
      override def afterTextChanged(s: Editable) =
        items(vh.getLayoutPosition).body = editText.getText.toString
    })
    vh
  }

  override def onBindViewHolder(holder: VH, position: Int) {
    val item = items(position)
    holder.checkBox.setTag(position)
    holder.checkBox.setChecked(items(position).isChecked)
    holder.editText.setText(item.body)
    holder.editText.setTextColor(if (holder.checkBox.isChecked) 0xffcccccc else 0xff000000)
    holder.editText.setTag(position)
    holder.editText.setEnabled(!holder.checkBox.isChecked)
    val dragState = holder.getDragStateFlags
    val swipeState = holder.getSwipeStateFlags
    if (((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_UPDATED) != 0)
      || ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_IS_UPDATED) != 0)) {
      val bgResId =
      if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_ACTIVE) != 0) {
        R.drawable.bg_item_dragging_active_state
      } else if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_DRAGGING) != 0) {
        R.drawable.bg_item_dragging_state
      } else if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_IS_ACTIVE) != 0) {
        R.drawable.bg_item_swiping_active_state
      } else if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_SWIPING) != 0) {
        R.drawable.bg_item_swiping_state
      } else {
        R.drawable.bg_item_normal_state
      }
      holder.container.setBackgroundResource(bgResId)
    }
    holder.setSwipeItemSlideAmount(0)
    if (shouldRequestFocus && position == newRowPos) {
      holder.editText.isTarget = true
      newRowPos = -1
      shouldRequestFocus = false
    }
  }

  override def getItemCount = if (items == null) 0 else items.size

  override def onMoveItem(from: Int, to: Int) {
    if (from == to) return
    moveItem(from, to)
    notifyItemMoved(from, to)
  }

  def moveItem(from: Int, to: Int) =
    if (from != to) items.insert(to, items.remove(from))

  override def onCheckCanStartDrag(holder: VH, x: Int, y: Int): Boolean = {
    val containerView = holder.container
    val dragHandleView = holder.dragHandle
    val offsetX = containerView.getLeft + (ViewCompat.getTranslationX(containerView) + 0.5f).asInstanceOf[Int]
    val offsetY = containerView.getTop + (ViewCompat.getTranslationY(containerView) + 0.5f).asInstanceOf[Int]
    ViewUtil.hitTest(dragHandleView, x - offsetX, y - offsetY)
  }

  override def onGetSwipeReactionType(holder: VH, x: Int, y: Int): Int = {
    if (onCheckCanStartDrag(holder, x, y))
      RecyclerViewSwipeManager.REACTION_CAN_NOT_SWIPE_BOTH
    else
      RecyclerViewSwipeManager.REACTION_CAN_SWIPE_RIGHT
  }

  override def onSetSwipeBackground(holder: VH, _type: Int) {
    val bgRes =
      if (_type == RecyclerViewSwipeManager.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND)
        R.drawable.bg_swipe_item_neutral
      else if (_type == RecyclerViewSwipeManager.DRAWABLE_SWIPE_LEFT_BACKGROUND)
        R.drawable.bg_swipe_item_left
      else if (_type == RecyclerViewSwipeManager.DRAWABLE_SWIPE_RIGHT_BACKGROUND)
        R.drawable.bg_swipe_item_right

    holder.itemView.setBackgroundResource(bgRes.asInstanceOf[Int])
  }

  override def onSwipeItem(holder: VH, result: Int): Int =
    if (result == RecyclerViewSwipeManager.RESULT_SWIPED_RIGHT)
      RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM
    else if (result == RecyclerViewSwipeManager.RESULT_SWIPED_LEFT)
      RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION
    else if (result == RecyclerViewSwipeManager.RESULT_CANCELED)
      RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_DEFAULT
    else
      RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_DEFAULT

  override def onPerformAfterSwipeReaction(holder: VH, result: Int, reaction: Int) {
    val position = holder.getAdapterPosition
    val item = items(position)
    if (reaction == RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM) {
      items.remove(position)
      notifyItemRemoved(position)
    } else if (reaction == RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION) {
      item.setPinnedToSwipeLeft(true)
      notifyItemChanged(position)
    } else {
      item.setPinnedToSwipeLeft(false)
    }
  }

  def appendNewRow(pos: Int) = {
    val isNewRow = pos == 0
    shouldRequestFocus = true
    newRowPos = pos
    items.append(new CheckItem())
    notifyItemInserted(pos)
    if (!isNewRow) // scroll to bottom
      recyclerView.getLayoutManager.scrollToPosition(pos)
  }

}