package com.android.perrier1034.post_it_note.ui.adapters

import android.support.v4.app.{Fragment, FragmentManager, FragmentPagerAdapter}
import android.support.v4.view.{PagerAdapter, ViewPager}
import com.android.perrier1034.post_it_note.model.PageRealm
import com.android.perrier1034.post_it_note.ui.PageFragment

// Lives with PageManager's life cycle
class MainPagerAdapter(fm: FragmentManager, private var items: Seq[PageRealm])
  extends FragmentPagerAdapter(fm) {

  override def getCount = if (items == null) 0 else items.size

  // Don't call. This method should be limited to internal use.
  override def getItem(pos: Int) = PageFragment.newInstance(items(pos).id)

  // This implementation is needed in order to PagerSlidingTabStrip work properly
  override def getPageTitle(pos: Int) = items(pos).title

  // should not serve `items` itself
  def getAllItems = items.toBuffer

  // This is the only method can change `items`
  def setItems(a: Seq[PageRealm]) { items = a }

  override final def getItemPosition(obj: AnyRef): Int = {
    if (mNeedAllChange) PagerAdapter.POSITION_NONE
    else PagerAdapter.POSITION_UNCHANGED
  }

  // `mNeedAllChange` is used only here
  private var mNeedAllChange = false
  def restart() = {
    mNeedAllChange = true
    notifyDataSetChanged()
    mNeedAllChange = false
  }

  // called after backed from PageSettingActivity
  def removeAll(pager: ViewPager) {
    val co = getCount
    for (i <- 0 until co) {
      if (i <= co) {
        val f = instantiateItem(pager, i).asInstanceOf[Fragment]
        if (f != null) {
          val tra = f.getFragmentManager.beginTransaction()
          tra.remove(f).commit
        }
      }
    }
  }
}
