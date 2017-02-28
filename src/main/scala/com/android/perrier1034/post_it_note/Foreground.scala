package com.android.perrier1034.post_it_note

import android.os.Handler

object Foreground {
  def apply(task: => Unit) =
    new Handler(App.getInstance.getMainLooper).post(new Runnable() {
      def run() = task
    })
}

