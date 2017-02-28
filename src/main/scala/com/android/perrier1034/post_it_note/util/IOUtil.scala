package com.android.perrier1034.post_it_note.util

import java.io._
import java.nio.channels.FileChannel

import android.content.Context
import android.content.pm.{PackageManager, ApplicationInfo}
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.android.perrier1034.post_it_note.App

object IOUtil {

  def makeImageFile(name: String): File = {
    val f = new File(Environment.getExternalStorageDirectory + "/" + App.NAME + "/image/", name)
    if (!f.exists) {
      try {
        var dc = f.getParentFile.mkdirs
        dc= f.createNewFile
      } catch { case e: IOException => e.printStackTrace() }
    }
    f
  }

  def importDB(context: Context) {
    val state = Environment.getExternalStorageState
    state match {
      case Environment.MEDIA_MOUNTED | Environment.MEDIA_MOUNTED_READ_ONLY =>
        val curFile = context.getDatabasePath(App.DB_NAME)
        val exportDir = new File(Environment.getExternalStorageDirectory, App.NAME + "/backup")
        val exFile = new File(exportDir, curFile.getName)
        if (!exportDir.exists) {
          App.toastShort("There is no backup file!")
          return
        }
        try {
          if (!exFile.exists) {
            exFile.createNewFile
          }
          copyFile(exFile, curFile)
          App.toastShort("Imported Successfully!")
        }
        catch {
          case e: IOException =>
            App.toastShort("Import Failed!")
            e.printStackTrace()
        }
      case _ => App.toastShort("Could not access to SD card!")
    }
  }

  def exportDB(context: Context) {
    val state = Environment.getExternalStorageState
    state match {
      case Environment.MEDIA_MOUNTED_READ_ONLY =>
        App.toastShort("SD card is mounted read-only!")

      case Environment.MEDIA_MOUNTED =>
        val curFile = context.getDatabasePath(App.DB_NAME)
        val exportDir = new File(Environment.getExternalStorageDirectory, App.NAME + "/backup")
        val exFile = new File(exportDir, curFile.getName)
        if (!exportDir.exists) {
          exportDir.mkdirs
        }
        try {
          if (!exFile.exists) {
            exFile.createNewFile
          }
          copyFile(curFile, exFile)
          App.toastShort("Export Successful!")
        } catch {
          case e: IOException =>
            App.toastShort("Export Failed!")
            e.printStackTrace()
        }

      case _ => App.toastShort("Could not access to SD card!")
    }
  }

  def deleteSharedPreferences(appContext: Context) {
    try {
      val info: ApplicationInfo = appContext.getPackageManager.getApplicationInfo(appContext.getPackageName, 0)
      val dirPath = info.dataDir + File.separator + "shared_prefs" + File.separator
      val dir = new File(dirPath)
      if (dir.exists && dir.isDirectory) {
        val list: Array[String] = dir.list
        for (itemPath <- list) new File(dirPath + itemPath).delete
      } else {
        Log.d("tag", "NO FILE or NOT DIR")
      }
    } catch {
      case e: PackageManager.NameNotFoundException => e.printStackTrace()
    }
  }

  @throws(classOf[IOException])
  def copyFile(source: File, dest: File) {
    val inChannel: FileChannel = new FileInputStream(source).getChannel
    val outChannel: FileChannel = new FileOutputStream(dest).getChannel
    try {
      inChannel.transferTo(0, inChannel.size, outChannel)
    } finally {
      if (inChannel != null) inChannel.close()
      if (outChannel != null) outChannel.close()
    }
  }
  def getPath(uri: Uri, context: Context): String = {
    var filePath: String = null
    var cursor: Cursor = null
    try {
      cursor = context.getContentResolver
        .query(uri, Array[String](android.provider.MediaStore.MediaColumns.DATA), null, null, null)
      cursor.moveToFirst
      val columnIndex = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
      filePath = cursor.getString(columnIndex)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        filePath = uri.getPath
    } finally {
      if (cursor != null) cursor.close()
    }
    if (filePath == null) {
      filePath = uri.getPath
      Log.v("talon_file_path", filePath)
    }
    filePath
  }

  implicit class RichFile(val f: File) extends AnyVal {
    def write(str: String) = {
      if (!f.exists()) f.createNewFile()
      val file = new FileWriter(f, false)
      file.write(str + "\n")
      file.close()
    }
  }
}