package com.android.perrier1034.post_it_note.receiver

import android.app.{Notification, NotificationManager, PendingIntent}
import android.content.{BroadcastReceiver, Context, Intent}
import android.support.v4.app.NotificationCompat
import android.text.TextUtils
import com.android.perrier1034.post_it_note.db.dao.{PageStore, NoteStore}
import com.android.perrier1034.post_it_note.model.NoteRealm
import com.android.perrier1034.post_it_note.ui.PageManager
import com.android.perrier1034.post_it_note.util.AlarmUtil
import com.android.perrier1034.post_it_note.{App, R}

object AlarmBroadcastReceiver {
  val KEY_INTENT_ALARM_USUAL = "KEY_INTENT_USUAL"
  val KEY_INTENT_PAGE_ORDER = "KEY_INTENT_PAGE_ORDER"
}

class AlarmBroadcastReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, receivedIntent: Intent) {
    receivedIntent.getAction match {
      case Intent.ACTION_PACKAGE_REPLACED | Intent.ACTION_BOOT_COMPLETED =>
        NoteStore.rebootAlarmsRealm()

      case AlarmBroadcastReceiver.KEY_INTENT_ALARM_USUAL =>
        val id = receivedIntent.getLongExtra(AlarmUtil.alarmIntentDataKey, -1)
        val note = NoteStore.findByIdRealm(id)
        val pageOrder = PageStore .getPageOrderById(note.parentId)
        if (note.inRubbish != 1) {
          val nm = App.getInstance.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
          val notif = makeNotification(note, pageOrder)
          nm.notify(id.asInstanceOf[Int], notif)
        }
        NoteStore.clearAlarmRealm(id)
      case _ =>
    }
  }

  private def makeNotification(note: NoteRealm, pageOrder: Int): Notification = {
    val in = new Intent(App.getInstance, classOf[PageManager])
    in.putExtra(AlarmBroadcastReceiver.KEY_INTENT_PAGE_ORDER, pageOrder)
    val pi = PendingIntent.getActivity(App.getInstance, 0, in, PendingIntent.FLAG_UPDATE_CURRENT)
    val builder = new NotificationCompat.Builder(App.getInstance)
    val content = if (TextUtils.isEmpty(note.checkItems)) "(check list)" else note.content
    builder
      .setSmallIcon(R.drawable.ic_launcher)
      .setTicker(content)
      .setContentText(content)
      .setWhen(System.currentTimeMillis).setAutoCancel(true)
      .setContentIntent(pi)
      .setContentTitle(if (TextUtils.isEmpty(note.title)) "No title" else note.title)
      .build()
  }
}