package com.android.perrier1034.post_it_note.util

import java.util.{Calendar, TimeZone}

import android.app.{AlarmManager, PendingIntent}
import android.content.{Context, Intent}
import com.android.perrier1034.post_it_note.App
import com.android.perrier1034.post_it_note.receiver.AlarmBroadcastReceiver

object AlarmUtil {

  val alarmIntentDataKey = "alarmIntentDataKey "
  def set(id: Long, year: Int, month: Int, date: Int) = {
    val cal = Calendar.getInstance
    cal.setTimeInMillis(System.currentTimeMillis)
    cal.setTimeZone(TimeZone.getDefault)
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month)
    cal.set(Calendar.DAY_OF_MONTH, date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    val intent = new Intent(App.getInstance, classOf[AlarmBroadcastReceiver])
    intent.putExtra(alarmIntentDataKey, id)
    intent.setAction(AlarmBroadcastReceiver.KEY_INTENT_ALARM_USUAL)
    val pendingIntent = PendingIntent.getBroadcast(App.getInstance, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    val alarmManager = App.getInstance.getSystemService(Context.ALARM_SERVICE).asInstanceOf[AlarmManager]
    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis, pendingIntent)
  }

  def clear(noteId: Long) = {
    val intent = new Intent(App.getInstance, classOf[AlarmBroadcastReceiver])
    intent.putExtra(alarmIntentDataKey, noteId)
    val pendingIntent = PendingIntent.getBroadcast(App.getInstance, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    val alarmManager = App.getInstance.getSystemService(Context.ALARM_SERVICE).asInstanceOf[AlarmManager]
    alarmManager.cancel(pendingIntent)
  }
}
