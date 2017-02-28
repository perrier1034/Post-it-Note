package com.android.perrier1034.post_it_note.ui.navigation

import android.content.Context
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{BaseAdapter, ImageView, TextView}
import com.android.perrier1034.post_it_note.R

object DrawerAdapter {
  case class ClickableHolder(labelView: TextView, iconView: ImageView)
  case class SectionHolder(labelView: TextView)
}

class DrawerAdapter(items: Seq[IDrawerModel], ctx: Context) extends BaseAdapter {

  val mInflater = LayoutInflater.from(ctx)

  override def getItemId(pos: Int) = pos
  override def getItem(pos: Int) = items(pos)
  override def getCount = items.size
  override def getViewTypeCount = 2
  override def isEnabled(position: Int) = getItem(position).isEnabled
  override def getItemViewType(position: Int) = getItem(position) match {
    case ClickableDrawerModel(_, _, _, _) => 0
    case SectionDrawerModel(_, _) => 1
  }

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val model = getItem(position)
    model match {
      case ClickableDrawerModel(_, _, _, _) => getClickableView(convertView, parent, model)
      case SectionDrawerModel(_, _) => getSectionView(convertView, parent, model)
    }
  }

  private def getSectionView(convertView: View, parentView: ViewGroup, model: IDrawerModel): View = {
    val sectionDrawerModel = model.asInstanceOf[SectionDrawerModel]
    var sectionHolder: DrawerAdapter.SectionHolder = null
    var convertViewDest = convertView

    if (convertViewDest == null) {
      convertViewDest = mInflater.inflate(R.layout.drawer_row_section, parentView, false)
      val labelView = convertViewDest.findViewById(R.id.drawer_row_section_label).asInstanceOf[TextView]
      sectionHolder = new DrawerAdapter.SectionHolder(labelView)
      convertViewDest.setTag(sectionHolder)
    }

    if (sectionHolder == null)
      sectionHolder = convertViewDest.getTag.asInstanceOf[DrawerAdapter.SectionHolder]

    sectionHolder.labelView.setText(sectionDrawerModel.label)
    convertViewDest
  }

  private def getClickableView(convertView: View, parentView: ViewGroup, model: IDrawerModel): View = {
    val clickableDrawerModel = model.asInstanceOf[ClickableDrawerModel]
    var clickableHolder: DrawerAdapter.ClickableHolder = null
    var convertViewDest = convertView

    if (convertView == null) {
      convertViewDest = mInflater.inflate(R.layout.drawer_row_clickable, parentView, false)
      val labelView = convertViewDest.findViewById(R.id.drawer_row_clickable_label).asInstanceOf[TextView]
      val iconView = convertViewDest.findViewById(R.id.drawer_row_clickable_icon).asInstanceOf[ImageView]
      clickableHolder = new DrawerAdapter.ClickableHolder(labelView, iconView)

      clickableDrawerModel.iconResId match {
        case Some(id) => clickableHolder.iconView.setImageResource(id)
        case None => iconView.setVisibility(View.VISIBLE)
      }

      convertViewDest.setTag(clickableHolder)
    }

    if (clickableHolder == null)
      clickableHolder = convertView.getTag.asInstanceOf[DrawerAdapter.ClickableHolder]

    clickableHolder.labelView.setText(clickableDrawerModel.label)

    convertViewDest.setOnClickListener(new OnClickListener {
      def onClick(v: View) {
        clickableDrawerModel.call()
        notifyDataSetChanged()
      }
    })
    convertViewDest
  }

}