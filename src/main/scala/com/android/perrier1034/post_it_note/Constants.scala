package com.android.perrier1034.post_it_note

object Constants {

  // in dialog, count vertical
  val PAGER_COLOR_MAPPING = Array(
    // toolbar      // text    // under_line // status_bar
    Array(0xFFF44336, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFD32F2F), 
    Array(0xFFE91E63, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFC2185B), 
    Array(0xFF9C27B0, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF7B1FA2), 
    Array(0xFF673AB7, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF512DA8), 
    Array(0xFF3F51B5, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF303F9F), 
    Array(0xFF2196F3, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF0288D1),
    Array(0xFF03A9F4, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF0288D1),
    Array(0xFF00BCD4, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF0097A7),
    Array(0xFF009688, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF00796B),
    Array(0xFF4CAF50, 0xFF000000, 0xFFFFFFFF, 0xFF388E3C),
    Array(0xFF8BC34A, 0xFF000000, 0xFFFFFFFF, 0xFF689F38), 
    Array(0xFFCDDC39, 0xFF000000, 0xFFFFFFFF, 0xFFAFB42B), 
    Array(0xFFFFEB3B, 0xFF000000, 0xFF000000, 0xFFFBC02D), 
    Array(0xFFFFC107, 0xFF000000, 0xFFFFFFFF, 0xFFFFA000), 
    Array(0xFFFF9800, 0xFF000000, 0xFFFFFFFF, 0xFFF57C00), // Default
    Array(0xFFFF5722, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFE64A19), 
    Array(0xFF795548, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF5D4037), 
    Array(0xFF9E9E9E, 0xFF000000, 0xFF000000, 0xFF616161), 
    Array(0xFF607D8B, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF455A64), 
    Array(0xff000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xff000000)
  )
  
  val STATUS_BAR_COLOR_ACTION_MODE = 0xff616161
  val STATUS_BAR_COLOR_BASE = 0xff9E9E9E
  val TOOL_BAR_COLOR_BASE = 0xff9E9E9E
  val TEXT_COLOR_DATE_DEFAULT = 0xff888888
  val TEXT_COLOR_NOTE_ALARM = 0xffff0000
  val COLOR_CIRCLE_STROKE = 0xff444444
  val COLOR_ICS_BLUE = 0xff33b5e5
  val DRAWER_ITEM_NOT_SELECTED = 0x88111111
  val DEFAULT_TEXT_SIZE_NOTE_SMALL = 13.0f
  val DEFAULT_TEXT_SIZE_NOTE_MEDIUM = 16.0f
  val DEFAULT_TEXT_SIZE_NOTE_LARGE = 20.0f
  val DEFAULT_BG_COLOR_POS_BAR = 14
  val MAX_PAGE_COUNT = 10
  val MAX_CHECK_ITEM_COUNT = 100
  val DRAWER_OPEN_DELAY = 230
  val NOTE_ORDER_LAST_MODIFIED = "last_modified"
  val NOTE_ORDER_id = "id"
}