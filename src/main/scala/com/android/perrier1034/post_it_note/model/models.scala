package com.android.perrier1034.post_it_note.model

import android.text.TextUtils
import com.android.perrier1034.post_it_note.App
import io.realm.RealmObject
import io.realm.annotations.{Ignore, PrimaryKey}

// id は正しく実装しないとアイテム移動後に変なアニメーションをするので注意。
//@Table(name = "check_items", id = BaseColumns._ID)
//class CheckItem extends Model {
//
//  @Column(name = "parent_id")
//  var parentId: Int = 0
//
//  @Column(name = "body")
//  var body: String= null
//
//  @Column(name = "checked")
//  var isChecked: Int = null
//
//  def toggleCheckState() = isChecked = if (isChecked == 1) 0 else 1
//
//  override def toString: String = body.toString
//}


case class CheckItem(id: Long = App.genUniqueNum,
                     var body: String = null,
                     var isChecked: Boolean = false,
                     var isPinnedToSwipeLeft: Boolean = false) {

  def toggleCheckState() = isChecked = !isChecked
  def setPinnedToSwipeLeft(b: Boolean) = isPinnedToSwipeLeft= b
}

//class Page extends RealmObject {
//
//  var title: String = null
//
//  var pageOrder: Int = 0
//
//  var pageColorPos: Int = 0
//
//  var counter: Int = 0
//
//}

class PageRealm extends RealmObject {

  @PrimaryKey
  var id: Long = 0l

  var title: String = null

  var pageOrder: Int = 0

  var pageColorPos: Int = 0

  var counter: Int = 0

}

//@Table(name = "notes", id = BaseColumns._ID)
//class Note extends Model {
//
//  @Column(name = "parent_id")
//  var parentId: Int = 0
//
//  @Column(name = "update_time")
//  var updateTime: Double = 0
//
//  @Column(name = "alarm_time")
//  var alarmTime: String = null
//
//  @Column(name = "in_rubbish")
//  var inRubbish: Int = 0
//
//  @Column(name = "title")
//  var title: String = null//
//
//  @Column(name = "content")
//  var content: String = null
//
//  @Column(name = "check_items")
//  var checkItems: String = null//
//
//  @Column(name = "check_states")
//  var checkStates: String = null//
//
//  @Column(name = "thumbnail_names")
//  var thumbnailNames: String = null
//
//  @Column(name = "viewer_image_names")
//  var viewerImageNames: String = null
//
//  @Column(name = "primary_thumbnail_name")
//  var primaryThumbnailName: String = null
//
//  @Column(name = "prev_thumbnail_name")
//  var prevThumbnailNames: String = null
//
//  def isNote = TextUtils.isEmpty(checkItems)
//
//  def isCheckList = !isNote
//
//}
class NoteRealm extends RealmObject {

  var last_modified: Long = 0

  @PrimaryKey
  var id: Long = 0

  var parentId: Int = 0

  var alarmTime: String = null

  var inRubbish: Int = 0

  var title: String = null//

  var content: String = null

  var checkItems: String = null//

  var checkStates: String = null//

  var thumbnailNames: String = null

  var viewerImageNames: String = null

  var primaryThumbnailName: String = null

  var prevThumbnailNames: String = null

  @Ignore
  var isNote = TextUtils.isEmpty(checkItems)

  @Ignore
  var isCheckList = !isNote

}

