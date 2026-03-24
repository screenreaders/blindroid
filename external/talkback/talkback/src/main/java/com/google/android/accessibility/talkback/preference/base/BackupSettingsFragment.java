/*
 * Copyright (C) 2026 The Blindroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.preference.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Backup/export/import for BlindReader settings. */
public class BackupSettingsFragment extends TalkbackBaseFragment {
  private static final int BACKUP_VERSION = 1;
  private static final String JSON_KEY_VERSION = "version";
  private static final String JSON_KEY_TIMESTAMP = "timestamp";
  private static final String JSON_KEY_PREFERENCES = "preferences";
  private static final String JSON_KEY_TYPE = "type";
  private static final String JSON_KEY_VALUE = "value";
  private static final String TYPE_STRING = "string";
  private static final String TYPE_BOOLEAN = "boolean";
  private static final String TYPE_INT = "int";
  private static final String TYPE_LONG = "long";
  private static final String TYPE_FLOAT = "float";
  private static final String TYPE_STRING_SET = "string_set";

  private SharedPreferences prefs;
  private ActivityResultLauncher<Intent> exportFileLauncher;
  private ActivityResultLauncher<Intent> importFileLauncher;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    exportFileLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              Context context = getContext();
              if (context == null || result.getResultCode() != Activity.RESULT_OK) {
                return;
              }
              Intent data = result.getData();
              if (data == null || data.getData() == null) {
                PreferencesActivityUtils.announceText(
                    getString(R.string.pref_backup_export_failed), context);
                return;
              }
              writeBackupToUri(context, data.getData());
            });

    importFileLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              Context context = getContext();
              if (context == null || result.getResultCode() != Activity.RESULT_OK) {
                return;
              }
              Intent data = result.getData();
              if (data == null || data.getData() == null) {
                PreferencesActivityUtils.announceText(
                    getString(R.string.pref_backup_import_failed), context);
                return;
              }
              readBackupFromUri(context, data.getData());
            });
  }

  public BackupSettingsFragment() {
    super(R.xml.backup_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_backup_settings_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();
    if (context == null) {
      return;
    }
    prefs = SharedPreferencesUtils.getSharedPreferences(context);

    Preference exportPref = findPreference(getString(R.string.pref_backup_export_key));
    if (exportPref != null) {
      exportPref.setOnPreferenceClickListener(
          preference -> {
            exportBackup(context);
            return true;
          });
    }

    Preference importPref = findPreference(getString(R.string.pref_backup_import_key));
    if (importPref != null) {
      importPref.setOnPreferenceClickListener(
          preference -> {
            showImportDialog(context);
            return true;
          });
    }

    Preference exportFilePref = findPreference(getString(R.string.pref_backup_export_file_key));
    if (exportFilePref != null) {
      exportFilePref.setOnPreferenceClickListener(
          preference -> {
            launchExportToFile(context);
            return true;
          });
    }

    Preference importFilePref = findPreference(getString(R.string.pref_backup_import_file_key));
    if (importFilePref != null) {
      importFilePref.setOnPreferenceClickListener(
          preference -> {
            launchImportFromFile();
            return true;
          });
    }
  }

  private void exportBackup(Context context) {
    try {
      JSONObject payload = buildBackupJson();
      Intent share = new Intent(Intent.ACTION_SEND);
      share.setType("application/json");
      share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pref_backup_export_title));
      share.putExtra(Intent.EXTRA_TEXT, payload.toString(2));
      startActivity(Intent.createChooser(share, getString(R.string.pref_backup_export_title)));
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_export_success), context);
    } catch (JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_export_failed), context);
    }
  }

  private void launchExportToFile(Context context) {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    intent.putExtra(Intent.EXTRA_TITLE, buildDefaultFileName());
    exportFileLauncher.launch(intent);
  }

  private void launchImportFromFile() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    importFileLauncher.launch(intent);
  }

  private String buildDefaultFileName() {
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    return "blindreader-backup-" + format.format(new Date()) + ".json";
  }

  private void writeBackupToUri(Context context, Uri uri) {
    try {
      JSONObject payload = buildBackupJson();
      byte[] bytes = payload.toString(2).getBytes("UTF-8");
      try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
        if (output == null) {
          PreferencesActivityUtils.announceText(
              getString(R.string.pref_backup_export_failed), context);
          return;
        }
        output.write(bytes);
        output.flush();
      }
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_export_file_success), context);
    } catch (IOException | JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_export_failed), context);
    }
  }

  private void readBackupFromUri(Context context, Uri uri) {
    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
      if (input == null) {
        PreferencesActivityUtils.announceText(
            getString(R.string.pref_backup_import_failed), context);
        return;
      }
      String json = new String(readAllBytes(input), "UTF-8");
      importBackup(context, json);
    } catch (IOException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_import_failed), context);
    }
  }

  private byte[] readAllBytes(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[4096];
    int count;
    while ((count = input.read(data)) != -1) {
      buffer.write(data, 0, count);
    }
    return buffer.toByteArray();
  }

  private JSONObject buildBackupJson() throws JSONException {
    JSONObject root = new JSONObject();
    root.put(JSON_KEY_VERSION, BACKUP_VERSION);
    root.put(JSON_KEY_TIMESTAMP, System.currentTimeMillis());
    JSONObject prefsJson = new JSONObject();
    for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
      String key = entry.getKey();
      if (!isBackupEligible(key)) {
        continue;
      }
      JSONObject encoded = encodeValue(entry.getValue());
      if (encoded != null) {
        prefsJson.put(key, encoded);
      }
    }
    root.put(JSON_KEY_PREFERENCES, prefsJson);
    return root;
  }

  private JSONObject encodeValue(Object value) throws JSONException {
    if (value instanceof String) {
      return wrapValue(TYPE_STRING, value);
    }
    if (value instanceof Boolean) {
      return wrapValue(TYPE_BOOLEAN, value);
    }
    if (value instanceof Integer) {
      return wrapValue(TYPE_INT, value);
    }
    if (value instanceof Long) {
      return wrapValue(TYPE_LONG, value);
    }
    if (value instanceof Float) {
      return wrapValue(TYPE_FLOAT, value);
    }
    if (value instanceof Set) {
      JSONArray array = new JSONArray();
      for (Object item : (Set<?>) value) {
        if (item instanceof String) {
          array.put(item);
        }
      }
      JSONObject wrapper = new JSONObject();
      wrapper.put(JSON_KEY_TYPE, TYPE_STRING_SET);
      wrapper.put(JSON_KEY_VALUE, array);
      return wrapper;
    }
    return null;
  }

  private JSONObject wrapValue(String type, Object value) throws JSONException {
    JSONObject wrapper = new JSONObject();
    wrapper.put(JSON_KEY_TYPE, type);
    wrapper.put(JSON_KEY_VALUE, value);
    return wrapper;
  }

  private boolean isBackupEligible(String key) {
    return !TextUtils.isEmpty(key) && key.startsWith("pref_");
  }

  private void showImportDialog(Context context) {
    EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    input.setMinLines(8);
    input.setHint(getString(R.string.pref_backup_import_summary));
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_backup_import_title)
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> importBackup(context, input.getText().toString()))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void importBackup(Context context, String json) {
    if (TextUtils.isEmpty(json)) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_import_failed), context);
      return;
    }
    JSONObject root;
    try {
      root = new JSONObject(json);
    } catch (JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_import_failed), context);
      return;
    }
    JSONObject prefsJson = root.optJSONObject(JSON_KEY_PREFERENCES);
    if (prefsJson == null) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_import_failed), context);
      return;
    }

    SharedPreferences.Editor editor = prefs.edit();
    int applied = 0;
    Iterator<String> keys = prefsJson.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      if (!isBackupEligible(key)) {
        continue;
      }
      JSONObject item = prefsJson.optJSONObject(key);
      if (item == null) {
        continue;
      }
      if (applyEntry(editor, key, item)) {
        applied++;
      }
    }
    editor.apply();
    if (applied > 0) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_import_success, applied), context);
    } else {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_backup_import_empty), context);
    }
  }

  private boolean applyEntry(SharedPreferences.Editor editor, String key, JSONObject item) {
    String type = item.optString(JSON_KEY_TYPE, null);
    if (TextUtils.isEmpty(type) || !item.has(JSON_KEY_VALUE)) {
      return false;
    }
    switch (type) {
      case TYPE_STRING:
        editor.putString(key, item.optString(JSON_KEY_VALUE, ""));
        return true;
      case TYPE_BOOLEAN:
        editor.putBoolean(key, item.optBoolean(JSON_KEY_VALUE));
        return true;
      case TYPE_INT:
        editor.putInt(key, item.optInt(JSON_KEY_VALUE));
        return true;
      case TYPE_LONG:
        editor.putLong(key, item.optLong(JSON_KEY_VALUE));
        return true;
      case TYPE_FLOAT:
        editor.putFloat(key, (float) item.optDouble(JSON_KEY_VALUE));
        return true;
      case TYPE_STRING_SET:
        JSONArray array = item.optJSONArray(JSON_KEY_VALUE);
        if (array == null) {
          return false;
        }
        Set<String> set = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
          String value = array.optString(i, null);
          if (!TextUtils.isEmpty(value)) {
            set.add(value);
          }
        }
        editor.putStringSet(key, set);
        return true;
      default:
        return false;
    }
  }
}
