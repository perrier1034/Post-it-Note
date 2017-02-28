package com.android.perrier1034.post_it_note.ui.dialog

import android.app.AlertDialog.Builder
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.{AdapterView, ArrayAdapter}

object ListDialog {
  def newInstance(title: Option[String], listItems: Seq[ListItem], icon: Option[Int]): DialogFragment = {
    val bun = new Bundle
    title foreach { bun.putString("title", _) }
    bun.putSerializable("items", listItems.toArray)
    icon foreach { bun.putInt("icon", _)}
    val instance = new ListDialog
    instance.setArguments(bun)
    instance
  }
}

class ListDialog extends DialogFragment  {

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    def createLabelArray: Array[String] = getListItems map { _.label }
    def getListItems = getArguments.getSerializable("items").asInstanceOf[Array[ListItem]]

    val adapter = new ArrayAdapter[String](getActivity, android.R.layout.simple_list_item_1, createLabelArray)
    val builder = new Builder(getActivity).setAdapter(adapter, null)

    val title = getArguments.getString("title", null)
    if (title != null) builder.setTitle(getArguments.getString("title"))

    val icon = getArguments.getInt("icon", 0)
    if (icon > 0) builder.setIcon(icon)
    val dialog = builder.create()

    val listView = dialog.getListView
    listView.setOnItemClickListener(new OnItemClickListener {
      override def onItemClick(adapterView: AdapterView[_], view: View, i: Int, l: Long): Unit = {
        getListItems(i).execute()
        dismiss()
      }
    })
    dialog
  }

}
