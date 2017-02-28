package com.android.perrier1034.post_it_note.db.dao

import com.android.perrier1034.post_it_note.model.PageRealm
import com.android.perrier1034.post_it_note.{App, Constants}
import io.realm.{RealmResults, Realm}
import scala.collection.JavaConversions._

object PageStore {

  def newPageObj = Realm.getInstance(App.getInstance).createObject(classOf[PageRealm])
  def insertRealm(str: String, colorPos: Int) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    val page = realm.createObject(classOf[PageRealm])
    page.title = str
    page.pageColorPos = colorPos
    realm.commitTransaction()
  }

  def updateRealm(page: PageRealm, str: String, pareOrder: Int, colorPos: Int) = {
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    page.title = str
    page.pageColorPos = colorPos
    realm.commitTransaction()
  }
  def allPagesRealmAsync: RealmResults[PageRealm] = {
    Realm.getInstance(App.getInstance)
      .where(classOf[PageRealm])
      .findAllAsync()
  }


  /**
   * This is called only one time in Application's initial boot.
   */
  def createPrimaryPageRealm() = {
    App.L(" createPrimaryPageRealm called")
    val realm = Realm.getInstance(App.getInstance)
    realm.beginTransaction()
    val page = realm.createObject(classOf[PageRealm])
    page.title = "ALL"
    page.pageColorPos = Constants.DEFAULT_BG_COLOR_POS_BAR
    page.pageOrder = 0
    realm.commitTransaction()
  }

  def deleteRealm(deadPages: Set[PageRealm]) {
    deadPages foreach { page =>
      NoteStore.deleteAllRealm(page.id.toInt)
      page.removeFromRealm()
    }
  }

  def getPageOrderByIdRealm(id: Int) = {
    val page = Realm.getInstance(App.getInstance).where(classOf[PageRealm])
      .equalTo("id", id.asInstanceOf[Integer])
      .findFirst()
    page.pageOrder
  }

  def getPageOrderById(id: Int) = {
    Realm.getInstance(App.getInstance)
      .where(classOf[PageRealm])
      .equalTo("id", id.asInstanceOf[Integer])
      .findFirst().pageOrder
  }

}
