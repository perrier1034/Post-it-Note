package com.android.perrier1034.post_it_note.db.dao

import android.content.ContentUris
import android.net.Uri
import com.android.perrier1034.post_it_note.App
import com.android.perrier1034.post_it_note.model.NoteRealm
import com.android.perrier1034.post_it_note.util.AlarmUtil
import io.realm.{Sort, Realm}
import scala.collection.JavaConversions._

object NoteStore {

  def newNoteObj = Realm.getInstance(App.getInstance).createObject(classOf[NoteRealm])

  def filteredNoteRealm(constraint: String, pageId: Int): Seq[NoteRealm] = {
    val query = Realm.getInstance(App.getInstance).where(classOf[NoteRealm])
    if (constraint != null) {
      query
        .beginGroup()
        .contains("content", constraint).or().contains("title", constraint)
        .endGroup()
    }
    if (pageId != 1) query.equalTo("parentId", pageId.asInstanceOf[Integer])
    query.equalTo("inRubbish", 0.asInstanceOf[Integer]).findAll()
  }

  def notesByPageIdRealmAsync(pageId: Int, order: String) = {
    val query = Realm.getInstance(App.getInstance).where(classOf[NoteRealm]).
      equalTo("inRubbish", 0.asInstanceOf[Integer])
    if (pageId != 1) {
      query.equalTo("parentId", pageId.asInstanceOf[Integer])
    }
    query.findAllSortedAsync(order, Sort.DESCENDING)
  }


  def getInitialNotesRealm(pageId: Int) = {
    Realm.getInstance(App.getInstance).where(classOf[NoteRealm])
      .equalTo("parentId", pageId.asInstanceOf[Integer])
      .equalTo("inRubbish", 0.asInstanceOf[Integer])
      .findAll()
  }


  def getInitialRubbishNotesRealmAsync = {
    Realm.getInstance(App.getInstance).where(classOf[NoteRealm])
      .equalTo("inRubbish", 1.asInstanceOf[Integer])
      .findAllSortedAsync("id", Sort.DESCENDING)
  }

  def deleteRealm(note: NoteRealm) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    // remove
    note.removeFromRealm()
    realm.commitTransaction()
  }

  def deleteAllRealm(pageId: Int) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    // remove
    realm.where(classOf[NoteRealm]).equalTo("parentId", pageId.asInstanceOf[Integer]).findAll.clear()
    realm.commitTransaction()
  }

  def clearAlarmRealm(note: NoteRealm) = {
    AlarmUtil.clear(note.id)
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    note.alarmTime = null
    realm.commitTransaction()
  }

  def findByPageId(pageId: Int) = {
    Realm.getInstance(App.getInstance).where(classOf[NoteRealm])
      .equalTo("parentId", pageId.asInstanceOf[Integer])
      .findAll()
  }


  private def clearAlarmOnPageDeletedRealm(pageId: Int) {
    val notes: Seq[NoteRealm] = findByPageId(pageId)
    notes foreach { note => AlarmUtil.clear(note.id) }
  }


  /**
   * This runs in caller's thread before notification is made.
   */
  def findByIdRealm(id: Long): NoteRealm = {
    Realm.getInstance(App.getInstance).where(classOf[NoteRealm])
      .equalTo("id", id.asInstanceOf[Integer])
      .findFirst()
  }

  /**
   * This runs in caller's thread before notification is made.
   */
//  def findByUriRealm(uri: Uri): NoteRealm = {
//    val id = ContentUris.parseId(uri)
//    findByIdRealm(id)
//  }

  /**
   * This runs in caller's thread before notification is made.
   */
//  def findByUri(uri: Uri): Note =
//    new Select().from(classOf[Note]).where(s"_id = ${ContentUris.parseId(uri)}").executeSingle()

  /**
   * This runs in caller's thread.
   * Will be called after NotificationManager#notify() to remove red alarm in main row.
   */
  def clearAlarmRealm(id: Long) {
    val note = findByIdRealm(id)
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    note.alarmTime = null
    realm.commitTransaction()
  }

  /**
   * This runs in caller's thread.
   * Will be called after NotificationManager#notify() to remove red alarm in main row.
   */
  /**
   * This runs in caller's thread.
   * Will be called after NotificationManager#notify() to remove red alarm in main row.
   */
//  def clearAlarmRealm(uri: Uri) {
//    val id = ContentUris.parseId(uri)
//    val note = findById(id)
//    val realm = Realm.getInstance(App.getInstance)
//    realm.beginTransaction()
//    note.alarmTime = null
//    realm.commitTransaction()
//  }
  /**
   * This runs in caller's thread.
   */
  def rebootAlarmsRealm() {
    def str2date(str: String): (Int, Int, Int) = {
      val strArray = str.split("/", 0)
      (strArray(0).toInt, strArray(1).toInt, strArray(2).toInt)
    }
    val notes: Seq[NoteRealm] =
      Realm.getInstance(App.getInstance).where(classOf[NoteRealm])
        .isNotNull("alarmTime")
        .findAll()
    notes foreach { note =>
      val (y, m, d) = str2date(note.content)
      AlarmUtil.set(note.id, y, m - 1, d)
    }
  }
  def moveToRubbishRealm(items: Seq[NoteRealm]) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    items foreach { note =>
      note.inRubbish = 1
    }
    realm.commitTransaction()
  }


  def transferRealm(notes: Seq[NoteRealm], pageId: Int) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    notes foreach { note =>
      note.parentId = pageId
    }
    realm.commitTransaction()
  }

  def moveToRubbishRealm(note: NoteRealm) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    note.inRubbish = 1
    realm.commitTransaction()
  }


  def resurrectFromRubbishRealm(note: NoteRealm) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    note.inRubbish = 0
    realm.commitTransaction()
  }


  def resurrectRealm(items: Seq[NoteRealm]) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    items.foreach { note =>
      note.inRubbish = 0
    }
    realm.commitTransaction()
  }


  def cleanRubbishRealm() = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    Realm.getInstance(App.getInstance).where(classOf[NoteRealm])
      .equalTo("inRubbish", 1.asInstanceOf[Integer])
      .findAll().clear()
    realm.commitTransaction()
  }

  def destroySelectedNotesRealm(items: Seq[NoteRealm]) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    items foreach { _.removeFromRealm() }
    realm.commitTransaction()
  }

}
