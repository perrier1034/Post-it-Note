/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.android.perrier1034.post_it_note.util

import android.support.v4.view.ViewCompat
import android.view.View

object ViewUtil {
  def hitTest(v: View, x: Int, y: Int): Boolean = {
    val tx: Int = (ViewCompat.getTranslationX(v) + 0.5f).asInstanceOf[Int]
    val ty: Int = (ViewCompat.getTranslationY(v) + 0.5f).asInstanceOf[Int]
    val left: Int = v.getLeft + tx
    val right: Int = v.getRight + tx
    val top: Int = v.getTop + ty
    val bottom: Int = v.getBottom + ty
    (x >= left) && (x <= right) && (y >= top) && (y <= bottom)
  }
}