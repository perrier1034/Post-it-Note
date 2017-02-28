package com.android.perrier1034.post_it_note.util

import java.io.{FileOutputStream, ByteArrayOutputStream, File, IOException}
import java.util.UUID

import android.app.Activity
import android.content.Context
import android.graphics.{Bitmap, BitmapFactory, Matrix}
import android.media.{ThumbnailUtils, ExifInterface}
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.android.perrier1034.post_it_note.App
import com.android.perrier1034.post_it_note.model.ImageInfo

object ImageUtil {

  def newThumbnail(orig: Bitmap, activity: Activity): Bitmap = {
    val dispScale = App.getDisplayScale(activity)
    val size = (156 * dispScale + 0.5f).toInt
    ThumbnailUtils.extractThumbnail(orig, size, size)
  }

  def saveBmpPair2external(infoSeq: Seq[ImageInfo]) {
    val dir = new File(Environment.getExternalStorageDirectory, App.NAME + "/image")
    if (!dir.exists) dir.mkdirs

    val (removed, living) = infoSeq.partition{ _.isRemoved }

    removed foreach { info =>
      val a = new File(dir, info.origImName)
      val b = new File(dir, info.thumbnailName)

      if (a.exists) a.delete
      if (b.exists) b.delete
    }

    living filter(_.isNew) foreach { info =>
      val fOrig  = new File(dir, info.origImName)
      val fThumb = new File(dir, info.thumbnailName)

      // create couple of File object
      try {
        fOrig.createNewFile
        fThumb.createNewFile
      } catch { case e: IOException => e.printStackTrace() }

      // export thumbnail
      val baosOrig = new ByteArrayOutputStream
      info.getOrigBmp.compress(Bitmap.CompressFormat.JPEG, 100, baosOrig)
      val byteArrOrig = baosOrig.toByteArray
      try {
        val fosOrig = new FileOutputStream(fOrig)
        fosOrig.write(byteArrOrig)
        fosOrig.flush()
        fosOrig.close()
      } catch { case e: IOException => e.printStackTrace() }

      // export original
      val baosThumb = new ByteArrayOutputStream
      info.getThumbnailBmp.compress(Bitmap.CompressFormat.JPEG, 100, baosThumb)
      val byteArrThumb = baosThumb.toByteArray
      try {
        val fosThumb = new FileOutputStream(fThumb)
        fosThumb.write(byteArrThumb)
        fosThumb.flush()
        fosThumb.close()
      } catch { case e: IOException => e.printStackTrace() }
    }

}

  def cropSquare(curImage: Bitmap): Bitmap = {
    var destImage: Bitmap = null
    if (curImage.getWidth >= curImage.getHeight) {
      destImage = Bitmap.createBitmap(
        curImage,
        curImage.getWidth / 2 - curImage.getHeight / 2,
        0,
        curImage.getHeight,
        curImage.getHeight)
    } else {
      destImage = Bitmap.createBitmap(
        curImage,
        0,
        curImage.getHeight / 2 - curImage.getWidth / 2,
        curImage.getWidth,
        curImage.getWidth)
    }
    curImage
  }

  def getBmpByUri(uri: Uri): Bitmap = {
    var orig: Bitmap = null
    try {
      orig = ImageUtil.getOrigBmpFromExternal(uri, App.getInstance)
    } catch {
      case e: IOException => e.printStackTrace()
    }
    orig
  }

  def getOrigBmpByName(filename: String): Bitmap = {
    val file = createDirInExternal(App.getInstance, "/image/" + filename)
    getOrigBmpFromExternal(Uri.fromFile(file), App.getInstance)
  }

  def getOrigBmpFromExternal(uri: Uri, context: Context): Bitmap = {
    val input = context.getContentResolver.openInputStream(uri)
    val reqWidth = 750
    val reqHeight = 750
    var byteArr = new Array[Byte](0)
    val buffer = new Array[Byte](1024)
    var len = 0
    var count = 0
    try {
      while ({ len = input.read(buffer); len } > -1) {
        if (len != 0) {
          if (count + len > byteArr.length) {
            val newbuf = new Array[Byte]((count + len) * 2)
            System.arraycopy(byteArr, 0, newbuf, 0, count)
            byteArr = newbuf
          }
          System.arraycopy(buffer, 0, byteArr, count, len)
          count += len
        }
      }
      val options = new BitmapFactory.Options
      options.inJustDecodeBounds = true
      BitmapFactory.decodeByteArray(byteArr, 0, count, options)
      options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
      options.inJustDecodeBounds = false
      options.inPreferredConfig = Bitmap.Config.ARGB_8888
      val b = BitmapFactory.decodeByteArray(byteArr, 0, count, options)
      val exif: ExifInterface = new ExifInterface(IOUtil.getPath(uri, context))
      val orientation: Int = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
      input.close()
      rotateBitmap(b, orientation)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        null
    }
  }

  @throws(classOf[IOException])
  def createDirInExternal(context: Context, name: String): File = {
    val state = Environment.getExternalStorageState
    var exportDir: File = null
    state match {
      case Environment.MEDIA_MOUNTED_READ_ONLY =>
        Toast.makeText(context, "SD card is mounted read-only!", Toast.LENGTH_SHORT).show()
      case Environment.MEDIA_MOUNTED =>
        exportDir = new File(Environment.getExternalStorageDirectory, App.NAME + name)
        if (!exportDir.exists) {
          if (!exportDir.mkdirs) {
            throw new IOException("External Storage is mounted correctly, but couldn't mkdir!")
          }
        }
      case _ =>
        Toast.makeText(context, "Could not access to SD card!", Toast.LENGTH_SHORT).show()
    }
    exportDir
  }

  def bitmap2byte(bmp: Bitmap): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos)
    bos.toByteArray
  }

  def rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap = {
    Log.v("composing_image", "rotation: " + orientation)
    try {
      val matrix = new Matrix
      orientation match {
        case ExifInterface.ORIENTATION_NORMAL =>
          return bitmap
        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL =>
          matrix.setScale(-1, 1)
        case ExifInterface.ORIENTATION_ROTATE_180 =>
          matrix.setRotate(180)
        case ExifInterface.ORIENTATION_FLIP_VERTICAL =>
          matrix.setRotate(180)
          matrix.postScale(-1, 1)
        case ExifInterface.ORIENTATION_TRANSPOSE =>
          matrix.setRotate(90)
          matrix.postScale(-1, 1)
        case ExifInterface.ORIENTATION_ROTATE_90 =>
          matrix.setRotate(90)
        case ExifInterface.ORIENTATION_TRANSVERSE =>
          matrix.setRotate(-90)
          matrix.postScale(-1, 1)
        case ExifInterface.ORIENTATION_ROTATE_270 =>
          matrix.setRotate(-90)
        case _ =>
          return bitmap
      }
      try {
        val bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth, bitmap.getHeight, matrix, true)
        bitmap.recycle()
        return bmRotated
      } catch {
        case e: OutOfMemoryError =>
          e.printStackTrace()
          return null
      }
    } catch { case e: Exception => e.printStackTrace() }
    bitmap
  }

  def calculateInSampleSize(opt: BitmapFactory.Options, reqW: Int, reqH: Int): Int = {
    val h = opt.outHeight
    val w = opt.outWidth
    var inSampleSize = 1
    if (h > reqH || w > reqW) {
      val halfH = h / 2
      val halfW = w / 2
      while ((halfH / inSampleSize) > reqH && (halfW / inSampleSize) > reqW) {
        inSampleSize *= 2
      }
    }
    inSampleSize
  }
}