package com.android.perrier1034.post_it_note

import java.util.concurrent.Executors

object Background {
  def apply(task: => Unit) =
    new Thread(new Runnable { def run() = task }).start()
}

// This is not used
object TaskQueue {
  private lazy val execService = Executors.newFixedThreadPool(4)
  def addTask(task: Runnable) = execService.submit(task)
}