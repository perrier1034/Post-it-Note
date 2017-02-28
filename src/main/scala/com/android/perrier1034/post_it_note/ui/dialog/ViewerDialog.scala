package com.android.perrier1034.post_it_note.ui.dialog

import android.app.{Dialog, DialogFragment}
import android.graphics.Bitmap
import android.os.{Bundle, Parcelable}
import android.support.v4.view.PagerAdapter
import android.view.{View, ViewGroup}
import com.android.perrier1034.post_it_note.R
import com.android.perrier1034.post_it_note.ui.views.LockableViewPager
import uk.co.senab.photoview.PhotoView

object ViewerDialog {
  private val KEY_BMPS: String = "0"
  private val KEY_DEFAULT_POS: String = "1"

  def newInstance(bmps: Array[Bitmap], defaultPos: Int): ViewerDialog = {
    val instance = new ViewerDialog
    val bun = new Bundle
    bun.putParcelableArray(KEY_BMPS, bmps.asInstanceOf[Array[Parcelable]])
    bun.putInt(KEY_DEFAULT_POS, defaultPos)
    instance.setArguments(bun)
    instance
  }
}

class ViewerDialog extends DialogFragment {

  def getBmps: Array[Bitmap] =
    getArguments.getParcelableArray(ViewerDialog.KEY_BMPS).asInstanceOf[Array[Bitmap]]

  def getDefalutPosition: Int =
    getArguments.getInt(ViewerDialog.KEY_DEFAULT_POS)

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val vg = getActivity.getLayoutInflater.inflate(R.layout.dialog_photo_viewer, null).asInstanceOf[ViewGroup]
    val viewPager = vg.findViewById(R.id.view_pager).asInstanceOf[LockableViewPager]
    viewPager.setAdapter(new PagerAdapter() {

      override def getCount: Int = getBmps.length

      override def instantiateItem(container: ViewGroup, position: Int): View = {
        val photoView = new PhotoView(container.getContext)
        photoView.setImageBitmap(getBmps(position))
        container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        photoView
      }

      override def destroyItem(container: ViewGroup, position: Int, obj: AnyRef) =
        container.removeView(obj.asInstanceOf[View])

      override def isViewFromObject(view: View, obj: AnyRef): Boolean =
        view eq obj

    })
    val d = new Dialog(getActivity, R.style.TransparentDialog)
    d.setContentView(vg)
    viewPager.setCurrentItem(getDefalutPosition)
    d
  }
}