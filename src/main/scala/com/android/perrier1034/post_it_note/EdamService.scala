package com.android.perrier1034.post_it_note

import java.io.{File, PrintWriter, ByteArrayOutputStream}
import java.security.MessageDigest
import com.android.perrier1034.post_it_note.model.NoteRealm
import com.android.perrier1034.post_it_note.util.SeqUtil._

import android.app.Activity
import com.android.perrier1034.post_it_note.util.IOUtil
import com.evernote.client.android.asyncclient.EvernoteCallback

import scala.collection.JavaConversions._
import android.graphics.Bitmap
import android.support.v4.app.FragmentManager
import android.text.TextUtils
import com.android.perrier1034.post_it_note.ui.dialog.{ListDialog, ListItem}
import com.android.perrier1034.post_it_note.util.ImageUtil
import com.evernote.client.android._
import com.evernote.edam.`type`.{Note => ENote, ResourceAttributes, Resource, Data, Notebook}

object EdamService {

  lazy val session = new EvernoteSession.Builder(App.getInstance)
    .setEvernoteService(EvernoteSession.EvernoteService.PRODUCTION)
    .setSupportAppLinkedNotebooks(true)
    .build("x", "y")
    .asSingleton()

  def noteFooter = "</en-note>"
  def noteStoreUrl = "https://www.evernote.com"
  def noteHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
    " <!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml.dtd\"> <en-note>"

  def listNotebooks: Either[Exception, List[Notebook]] =
    try { Right(getNoteStoreClient.listNotebooks().toList) }
    catch { case e: Exception => Left(e) }

  def saveNotesRealm(notes: Seq[NoteRealm], fm: FragmentManager, onItemClicked: Option[() => Unit]) = {
    Background {
      listNotebooks match {
        case Right(notebooks) =>
          val listItems = notebooks.indices map { i =>
            new ListItem(notebooks(i).getName) { def execute() = {
              onItemClicked foreach { _() }
              App.toastShort(R.string.toast_edam_saving)
              Background{ notes2ENotesRealm(notes).foreach{ saveNote(_, notebooks(i)) } }
            }}
          }
          Foreground {
            ListDialog.newInstance(Some("Save in Evernote"), listItems, Some(R.drawable.edam)).show(fm, "saveNotes")
          }

        case Left(e) =>
          App.toastShort("Saving note failed")
      }
    }
  }
  def isLoggedIn = session.isLoggedIn
  def login(activity: Activity) = session.authenticate(activity)
  def logout = session.logOut()

  private def getNoteStoreClient =
    session.getEvernoteClientFactory.getNoteStoreClient

  private def saveNote(note: ENote, nb: Notebook) = {
    if (!TextUtils.isEmpty(nb.getGuid)) note.setNotebookGuid(nb.getGuid)
      session.getEvernoteClientFactory.getNoteStoreClient.createNote(note)
  }

  private def notes2ENotesRealm(notes: Seq[NoteRealm]): Seq[ENote] = {
    notes map { note =>
      val eNote = new ENote
      if (!TextUtils.isEmpty(note.checkItems)) {
        // body
        val b = new StringBuilder
        b.append(EdamService.noteHeader)
        (note.checkItems.split(','), note.checkItems).zipped foreach { case (text, state) =>
          b.append(s"""<en-todo checked="${state == '1'}"/> $text <br/>""")
        }
        eNote.setContent(b.append(EdamService.noteFooter).toString())

        // title
        if (TextUtils.isEmpty(note.title)) {
          eNote.setTitle(App.getInstance.getText(R.string.edam_no_title).toString)
        } else {
          eNote.setTitle(note.title)
        }
        eNote
      } else {
        // body
        val builder = new StringBuilder
        builder.append(noteHeader).append(note.content).append("\n\n")
        // images
        if (!TextUtils.isEmpty(note.thumbnailNames)) {
          zipEach(note.thumbnailNames.split(","), note.viewerImageNames.split(",")){ (thumb, orig) =>
            val bosOrig = new ByteArrayOutputStream
            ImageUtil.getOrigBmpByName(orig).compress(Bitmap.CompressFormat.JPEG, 100, bosOrig)
            val byteArrOrig = bosOrig.toByteArray
            val data = new Data
            data.setSize(byteArrOrig.length)
            try data.setBodyHash(MessageDigest.getInstance("MD5").digest(byteArrOrig))
            catch { case e: Exception => e.printStackTrace() }
            data.setBody(byteArrOrig)
            val res = new Resource
            res.setData(data)
            res.setMime("image/jpg")

            val attr = new ResourceAttributes
            attr.setFileName(orig)
            res.setAttributes(attr)
            eNote.addToResources(res)
            val hashHex1 = App.bytes2hex(res.getData.getBodyHash)
            builder.append("<en-media type=\"image/jpeg\" hash=\"" + hashHex1 + "\"/>")
            eNote.addToResources(res)
          }
        }
        eNote.setContent(builder.append(EdamService.noteFooter).toString())

        // title
        if (!TextUtils.isEmpty(note.title))
          eNote.setTitle(note.title)
        else
          eNote.setTitle(App.getInstance.getText(R.string.edam_no_title).toString)

        eNote
      }
    }
  }

}
