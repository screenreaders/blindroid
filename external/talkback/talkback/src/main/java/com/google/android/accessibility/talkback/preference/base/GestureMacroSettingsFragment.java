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
import androidx.preference.EditTextPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.gesture.GestureMacroStore;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Preferences for gesture macros. */
public class GestureMacroSettingsFragment extends TalkbackBaseFragment {

  private Preference macro1ActionsPreference;
  private Preference macro2ActionsPreference;
  private Preference macro3ActionsPreference;
  private Preference exportMacrosPreference;
  private Preference importMacrosPreference;
  private Preference exportMacrosFilePreference;
  private Preference importMacrosFilePreference;
  private Preference copyMacrosPreference;
  private ActivityResultLauncher<Intent> exportMacrosFileLauncher;
  private ActivityResultLauncher<Intent> importMacrosFileLauncher;

  public GestureMacroSettingsFragment() {
    super(R.xml.gesture_macro_preferences);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    exportMacrosFileLauncher =
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
                    getString(R.string.pref_macro_export_failed), context);
                return;
              }
              writeMacrosToUri(context, data.getData());
            });

    importMacrosFileLauncher =
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
                    getString(R.string.pref_macro_import_failed), context);
                return;
              }
              readMacrosFromUri(context, data.getData());
            });
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.pref_gesture_macro_settings_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    setupNameSummary(R.string.pref_macro_1_name_key, R.string.macro_default_name_1);
    setupNameSummary(R.string.pref_macro_2_name_key, R.string.macro_default_name_2);
    setupNameSummary(R.string.pref_macro_3_name_key, R.string.macro_default_name_3);
    macro1ActionsPreference =
        setupActionsPreference(R.string.pref_macro_1_actions_list_key, /* macroIndex= */ 1);
    macro2ActionsPreference =
        setupActionsPreference(R.string.pref_macro_2_actions_list_key, /* macroIndex= */ 2);
    macro3ActionsPreference =
        setupActionsPreference(R.string.pref_macro_3_actions_list_key, /* macroIndex= */ 3);

    exportMacrosPreference = findPreference(getString(R.string.pref_macro_export_key));
    if (exportMacrosPreference != null) {
      exportMacrosPreference.setOnPreferenceClickListener(
          pref -> {
            exportMacros(getContext());
            return true;
          });
    }

    importMacrosPreference = findPreference(getString(R.string.pref_macro_import_key));
    if (importMacrosPreference != null) {
      importMacrosPreference.setOnPreferenceClickListener(
          pref -> {
            showImportDialog(getContext());
            return true;
          });
    }

    exportMacrosFilePreference = findPreference(getString(R.string.pref_macro_export_file_key));
    if (exportMacrosFilePreference != null) {
      exportMacrosFilePreference.setOnPreferenceClickListener(
          pref -> {
            launchExportToFile(getContext());
            return true;
          });
    }

    importMacrosFilePreference = findPreference(getString(R.string.pref_macro_import_file_key));
    if (importMacrosFilePreference != null) {
      importMacrosFilePreference.setOnPreferenceClickListener(
          pref -> {
            launchImportFromFile();
            return true;
          });
    }

    copyMacrosPreference = findPreference(getString(R.string.pref_macro_copy_key));
    if (copyMacrosPreference != null) {
      copyMacrosPreference.setOnPreferenceClickListener(
          pref -> {
            showCopyDialog(getContext());
            return true;
          });
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    updateActionsSummary(macro1ActionsPreference, 1);
    updateActionsSummary(macro2ActionsPreference, 2);
    updateActionsSummary(macro3ActionsPreference, 3);
  }

  private void setupNameSummary(int keyResId, int defaultNameResId) {
    EditTextPreference preference = (EditTextPreference) findPreference(getString(keyResId));
    if (preference == null) {
      return;
    }
    preference.setSummaryProvider(
        pref -> {
          String value = preference.getText();
          if (TextUtils.isEmpty(value)) {
            return getString(defaultNameResId);
          }
          return value;
        });
  }

  private Preference setupActionsPreference(int keyResId, int macroIndex) {
    Preference preference = findPreference(getString(keyResId));
    if (preference == null) {
      return null;
    }
    updateActionsSummary(preference, macroIndex);
    preference.setOnPreferenceClickListener(
        pref -> {
          Intent intent = new Intent(getContext(), GestureMacroEditorActivity.class);
          intent.putExtra(GestureMacroEditorActivity.EXTRA_MACRO_INDEX, macroIndex);
          startActivity(intent);
          return true;
        });
    return preference;
  }

  private void updateActionsSummary(Preference preference, int macroIndex) {
    if (preference == null || getContext() == null) {
      return;
    }
    List<String> actions = GestureMacroStore.getActionList(getContext(), macroIndex);
    int count = actions.size();
    if (count == 0) {
      preference.setSummary(getString(R.string.pref_macro_actions_summary, count));
      return;
    }
    StringBuilder summary = new StringBuilder();
    int maxItems = 3;
    int added = 0;
    for (String action : actions) {
      if (TextUtils.isEmpty(action)) {
        continue;
      }
      if (summary.length() > 0) {
        summary.append(", ");
      }
      summary.append(GestureShortcutMapping.getActionString(getContext(), action));
      added++;
      if (added >= maxItems) {
        break;
      }
    }
    if (count > maxItems) {
      summary.append("…");
    }
    preference.setSummary(
        getString(R.string.pref_macro_actions_summary_with_list, count, summary.toString()));
  }

  private void exportMacros(Context context) {
    if (context == null) {
      return;
    }
    try {
      JSONObject payload = buildMacrosJson(context);
      Intent share = new Intent(Intent.ACTION_SEND);
      share.setType("application/json");
      share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pref_macro_export_title));
      share.putExtra(Intent.EXTRA_TEXT, payload.toString(2));
      startActivity(Intent.createChooser(share, getString(R.string.pref_macro_export_title)));
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_macro_export_success), context);
    } catch (JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_macro_export_failed), context);
    }
  }

  private void launchExportToFile(Context context) {
    if (context == null) {
      return;
    }
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    intent.putExtra(Intent.EXTRA_TITLE, buildFileName());
    exportMacrosFileLauncher.launch(intent);
  }

  private void launchImportFromFile() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    importMacrosFileLauncher.launch(intent);
  }

  private String buildFileName() {
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    return "blindreader-macros-" + format.format(new Date()) + ".json";
  }

  private void showImportDialog(Context context) {
    if (context == null) {
      return;
    }
    EditText input = new EditText(context);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    input.setMinLines(6);
    input.setHint(getString(R.string.pref_macro_import_summary));
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_macro_import_title)
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> importMacros(context, input.getText().toString()))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void importMacros(Context context, String json) {
    if (context == null) {
      return;
    }
    int imported = applyMacrosJson(context, json);
    if (imported > 0) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_macro_import_success, imported), context);
      updateActionsSummary(macro1ActionsPreference, 1);
      updateActionsSummary(macro2ActionsPreference, 2);
      updateActionsSummary(macro3ActionsPreference, 3);
    } else {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_macro_import_failed), context);
    }
  }

  private void writeMacrosToUri(Context context, Uri uri) {
    try {
      JSONObject payload = buildMacrosJson(context);
      byte[] bytes = payload.toString(2).getBytes("UTF-8");
      try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
        if (output == null) {
          PreferencesActivityUtils.announceText(
              getString(R.string.pref_macro_export_failed), context);
          return;
        }
        output.write(bytes);
        output.flush();
      }
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_macro_export_file_success), context);
    } catch (IOException | JSONException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_macro_export_failed), context);
    }
  }

  private void readMacrosFromUri(Context context, Uri uri) {
    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
      if (input == null) {
        PreferencesActivityUtils.announceText(
            getString(R.string.pref_macro_import_failed), context);
        return;
      }
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int read;
      while ((read = input.read(buffer)) != -1) {
        output.write(buffer, 0, read);
      }
      String json = output.toString("UTF-8");
      int imported = applyMacrosJson(context, json);
      if (imported > 0) {
        PreferencesActivityUtils.announceText(
            getString(R.string.pref_macro_import_file_success, imported), context);
        updateActionsSummary(macro1ActionsPreference, 1);
        updateActionsSummary(macro2ActionsPreference, 2);
        updateActionsSummary(macro3ActionsPreference, 3);
      } else {
        PreferencesActivityUtils.announceText(
            getString(R.string.pref_macro_import_failed), context);
      }
    } catch (IOException e) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_macro_import_failed), context);
    }
  }

  private void showCopyDialog(Context context) {
    if (context == null) {
      return;
    }
    final CharSequence[] macroNames = buildMacroNameList(context);
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_macro_copy_source_title)
        .setItems(
            macroNames,
            (dialog, which) -> showCopyTargetDialog(context, which + 1, macroNames))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showCopyTargetDialog(Context context, int sourceIndex, CharSequence[] macroNames) {
    CharSequence[] targetNames = new CharSequence[macroNames.length - 1];
    int[] targetIndices = new int[macroNames.length - 1];
    int cursor = 0;
    for (int i = 0; i < macroNames.length; i++) {
      int index = i + 1;
      if (index == sourceIndex) {
        continue;
      }
      targetNames[cursor] = macroNames[i];
      targetIndices[cursor] = index;
      cursor++;
    }
    A11yAlertDialogWrapper.alertDialogBuilder(context)
        .setTitle(R.string.pref_macro_copy_target_title)
        .setItems(
            targetNames,
            (dialog, which) -> copyMacro(context, sourceIndex, targetIndices[which], macroNames))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void copyMacro(
      Context context, int sourceIndex, int targetIndex, CharSequence[] macroNames) {
    if (sourceIndex == targetIndex) {
      PreferencesActivityUtils.announceText(
          getString(R.string.pref_macro_copy_failed), context);
      return;
    }
    List<String> actions = GestureMacroStore.getActionList(context, sourceIndex);
    GestureMacroStore.saveActionList(context, targetIndex, actions);
    PreferencesActivityUtils.announceText(
        getString(
            R.string.pref_macro_copy_announce,
            macroNames[sourceIndex - 1],
            macroNames[targetIndex - 1]),
        context);
    updateActionsSummary(macro1ActionsPreference, 1);
    updateActionsSummary(macro2ActionsPreference, 2);
    updateActionsSummary(macro3ActionsPreference, 3);
  }

  private CharSequence[] buildMacroNameList(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    CharSequence[] names = new CharSequence[3];
    for (int i = 0; i < 3; i++) {
      names[i] = GestureMacroEditorActivity.getMacroName(context, i + 1, prefs);
    }
    return names;
  }

  private JSONObject buildMacrosJson(Context context) throws JSONException {
    JSONObject payload = new JSONObject();
    payload.put("version", 1);
    JSONArray macros = new JSONArray();
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    for (int index = 1; index <= 3; index++) {
      JSONObject macro = new JSONObject();
      macro.put("index", index);
      String nameKey = context.getString(getMacroNameKey(index));
      String name = prefs.getString(nameKey, "");
      macro.put("name", name);
      List<String> actions = GestureMacroStore.getActionList(context, index);
      JSONArray array = new JSONArray();
      for (String action : actions) {
        if (!TextUtils.isEmpty(action)) {
          array.put(action);
        }
      }
      macro.put("actions", array);
      macros.put(macro);
    }
    payload.put("macros", macros);
    return payload;
  }

  private int applyMacrosJson(Context context, String json) {
    if (TextUtils.isEmpty(json)) {
      return 0;
    }
    JSONObject root;
    try {
      root = new JSONObject(json);
    } catch (JSONException e) {
      return 0;
    }
    JSONArray macros = root.optJSONArray("macros");
    if (macros == null) {
      return 0;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    int imported = 0;
    for (int i = 0; i < macros.length(); i++) {
      JSONObject macro = macros.optJSONObject(i);
      if (macro == null) {
        continue;
      }
      int index = macro.optInt("index", -1);
      if (index < 1 || index > 3) {
        continue;
      }
      String name = macro.optString("name", "");
      if (!TextUtils.isEmpty(name)) {
        prefs.edit().putString(context.getString(getMacroNameKey(index)), name).apply();
      }
      JSONArray actionsJson = macro.optJSONArray("actions");
      List<String> actions = new ArrayList<>();
      if (actionsJson != null) {
        for (int j = 0; j < actionsJson.length(); j++) {
          String value = actionsJson.optString(j, null);
          if (!TextUtils.isEmpty(value)) {
            actions.add(value);
          }
        }
      }
      GestureMacroStore.saveActionList(context, index, actions);
      imported++;
    }
    return imported;
  }

  private int getMacroNameKey(int index) {
    switch (index) {
      case 1:
        return R.string.pref_macro_1_name_key;
      case 2:
        return R.string.pref_macro_2_name_key;
      case 3:
      default:
        return R.string.pref_macro_3_name_key;
    }
  }
}
