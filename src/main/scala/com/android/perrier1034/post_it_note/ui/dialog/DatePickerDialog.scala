package com.android.perrier1034.post_it_note.ui.dialog

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.{ViewGroup, LayoutInflater, View}
import com.android.perrier1034.post_it_note.R
import com.prolificinteractive.materialcalendarview.{OnDateChangedListener, CalendarDay, MaterialCalendarView}

object DatePickerDialog {
  trait Listener extends Serializable {
    def call(y: Int, m: Int, d: Int)
  }
  def newInstance(listener: Listener) = {
    val instance = new DatePickerDialog
    val bun = new Bundle
    bun.putSerializable("listener", listener)
    instance.setArguments(bun)
    instance
  }
}

class DatePickerDialog extends DialogFragment with OnDateChangedListener {

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, state: Bundle) =
    inflater.inflate(R.layout.view_date_picker, container, false)

  override def onViewCreated(v: View, b: Bundle) {
    super.onViewCreated(v, b)

    val widget =  v.findViewById(R.id.calendarView).asInstanceOf[MaterialCalendarView]
    widget.setOnDateChangedListener(this)
  }

  override def onDateChanged(widget: MaterialCalendarView, date: CalendarDay) {
    getArguments.getSerializable("listener").asInstanceOf[DatePickerDialog.Listener]
    .call(date.getYear, date.getMonth, date.getDay)
    dismiss()
  }
}