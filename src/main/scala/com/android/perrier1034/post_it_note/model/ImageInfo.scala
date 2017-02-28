package com.android.perrier1034.post_it_note.model

import java.io.IOException

import android.graphics.Bitmap
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import com.android.perrier1034.post_it_note.App
import com.android.perrier1034.post_it_note.db.dao.NoteStore
import com.android.perrier1034.post_it_note.util.ImageUtil
import uk.co.senab.photoview.PhotoView

// very very ugly class
case class ImageInfo(origImName: String,
                     thumbnailName: String,
                     imageView: ImageView,
                     isNew: Boolean,
                     tap: (View, ImageInfo) => Unit,
                     longTap: (View, ImageInfo) => Unit) {

  // ビューア用のImageView
  private var mPhotoView: PhotoView = null

  // 本体画像のBitmap
  private var mOrigBmp: Bitmap = null

  // サムネイル画像のBitmap
  private var mThumbnailBmp: Bitmap = null

  var isRemoved = false

  imageView.setOnClickListener(new OnClickListener {
    override def onClick(v: View) = tap(v, ImageInfo.this)
  })

  imageView.setOnLongClickListener(new View.OnLongClickListener {
    override def onLongClick(v: View): Boolean = {
      longTap(v, ImageInfo.this)
      true
    }
  })

  def dispose() = {
    if (mOrigBmp != null) mOrigBmp.recycle()
    if (mThumbnailBmp != null) mThumbnailBmp.recycle()
    if (mPhotoView != null) mPhotoView.setImageBitmap(null)
    imageView.setImageBitmap(null)
    mOrigBmp = null
    mPhotoView = null
    isRemoved = true
  }


  def getThumbnailBmp: Bitmap = mThumbnailBmp

  def setThumbnailBmp(bmp: Bitmap) = mThumbnailBmp = bmp

  def getOrigBmp: Bitmap = {
    if (mOrigBmp == null) {
      try mOrigBmp = ImageUtil.getOrigBmpByName(origImName)
      catch { case e: IOException => e.printStackTrace() }
    }
    mOrigBmp
  }

  def setOrigBmp(bmp: Bitmap) = mOrigBmp = bmp

}