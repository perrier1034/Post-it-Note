package com.android.perrier1034.post_it_note.ui

class MultiSelectionSupport(listener: MultiSelectionSupport.Client) {

  val mSelectedItems = scala.collection.mutable.Set[Integer]()
  private var mIsActionMode: Boolean = false

  def startActionMode() {
    mIsActionMode = true
  }

  def isActionMode = mIsActionMode
  def isNotActionMode = !mIsActionMode

  def getSelectedItemPositions = mSelectedItems.toSeq

  def stopActionMode() {
    mIsActionMode = false
    mSelectedItems.clear()
    listener.onActionModeStopped()
  }

  def toggleSelection(pos: Int) {
    if (mSelectedItems.contains(pos))
      mSelectedItems -= pos
    else
      mSelectedItems += pos

    listener.onSelectionChanged()
  }

  def selectAll(countAll: Int) {
    (0 until countAll) foreach  { mSelectedItems += _ }
    listener.onSelectionChanged()
  }

  def unSelectAll() {
    mSelectedItems.clear()
    listener.onSelectionChanged()
  }

  def isSelected(pos: Int) = {
    getSelectedItemPositions.contains(pos)
  }

  def getSelectedItemCount = mSelectedItems.size

}

object MultiSelectionSupport {
  trait Client {
    def onActionModeStopped()
    def onSelectionChanged()
  }

}

