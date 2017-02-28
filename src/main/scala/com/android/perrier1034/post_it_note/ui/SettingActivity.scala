package com.android.perrier1034.post_it_note.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.{Preference, PreferenceFragment}
import com.android.perrier1034.post_it_note.ui.dialog.ConfirmDialog
import com.android.perrier1034.post_it_note.ui.dialog.ConfirmDialog.ClickListener
import com.android.perrier1034.post_it_note.util.IOUtil
import com.android.perrier1034.post_it_note.{App, R}

object SettingActivity {
  val REQUEST_CODE = 0x0
  val KEY_BUNDLE_DB_IMPORTED = "key_import"
  val KEY_BUNDLE_DB_BACKUP = "key_backup"
  var dbImported = false

  class SettingFragment extends PreferenceFragment {
    lazy val mPrefScreen = getPreferenceScreen
    lazy val mPrefBackup = mPrefScreen.findPreference(SettingActivity.KEY_BUNDLE_DB_BACKUP)
    lazy val mPrefImport = mPrefScreen.findPreference(SettingActivity.KEY_BUNDLE_DB_IMPORTED)

    override def onCreate(savedInstanceState: Bundle) {
      super.onCreate(savedInstanceState)
      addPreferencesFromResource(R.xml.pref)
      mPrefBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        override def onPreferenceClick(arg0: Preference): Boolean = {
          IOUtil.exportDB(App.getInstance)
          true
        }
      })
      mPrefImport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        override def onPreferenceClick(arg0: Preference): Boolean = {
          ConfirmDialog.newInstance(Some(getText(R.string.cdialog_title_import_db).toString),
            getText(R.string.cdialog_warning_import_db).toString, None, new ClickListener {
              def onClick() {
                IOUtil.importDB(App.getInstance)
                dbImported = true
              }
            }).show(getFragmentManager, "confDialog")
          true
        }
      })
    }

  }

}


class SettingActivity extends BaseActivity {
  override def init() = getFragmentManager.beginTransaction
    .replace(R.id.setting_content, new SettingActivity.SettingFragment).commit

  override def contentsResId = R.layout.activity_component_setting

  override def onBackPressed() = {
    if (SettingActivity.dbImported) {
      val in = new Intent
      val bu = new Bundle
      if (SettingActivity.dbImported) {
        bu.putBoolean(SettingActivity.KEY_BUNDLE_DB_IMPORTED, true)
        SettingActivity.dbImported = false
      }

      in.putExtras(bu)
      setResult(Activity.RESULT_OK, in)
    }
    super.onBackPressed()
  }
}
