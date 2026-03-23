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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.gesture.GestureMacroStore;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.List;

/** Activity for editing gesture macro actions. */
public class GestureMacroEditorActivity extends AppCompatActivity {

  public static final String EXTRA_MACRO_INDEX = "macro_index";

  private int macroIndex;
  private SharedPreferences prefs;
  private final List<String> actions = new ArrayList<>();
  private MacroActionAdapter adapter;
  private TextView emptyView;
  private TextView titleView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_gesture_macro_editor);

    prefs = SharedPreferencesUtils.getSharedPreferences(this);
    macroIndex = getIntent().getIntExtra(EXTRA_MACRO_INDEX, 1);

    titleView = findViewById(R.id.macro_editor_title);
    emptyView = findViewById(R.id.macro_editor_empty);
    RecyclerView recyclerView = findViewById(R.id.macro_action_list);
    Button addButton = findViewById(R.id.macro_action_add);

    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    adapter = new MacroActionAdapter(this, actions, this::onEditAction, this::onMoveUp, this::onMoveDown, this::onRemove);
    recyclerView.setAdapter(adapter);

    loadActions();
    updateTitle();
    updateEmptyState();

    addButton.setOnClickListener(v -> showActionPicker(-1));
  }

  private void loadActions() {
    actions.clear();
    actions.addAll(GestureMacroStore.getActionList(this, macroIndex));
    adapter.notifyDataSetChanged();
  }

  private void persistActions() {
    GestureMacroStore.saveActionList(this, macroIndex, actions);
    updateEmptyState();
  }

  private void updateTitle() {
    String name = getMacroName(this, macroIndex, prefs);
    titleView.setText(getString(R.string.macro_editor_title) + ": " + name);
  }

  private void updateEmptyState() {
    emptyView.setVisibility(actions.isEmpty() ? View.VISIBLE : View.GONE);
  }

  private void onEditAction(int index) {
    showActionPicker(index);
  }

  private void onMoveUp(int index) {
    if (index <= 0 || index >= actions.size()) {
      return;
    }
    String item = actions.remove(index);
    actions.add(index - 1, item);
    adapter.notifyItemMoved(index, index - 1);
    persistActions();
  }

  private void onMoveDown(int index) {
    if (index < 0 || index >= actions.size() - 1) {
      return;
    }
    String item = actions.remove(index);
    actions.add(index + 1, item);
    adapter.notifyItemMoved(index, index + 1);
    persistActions();
  }

  private void onRemove(int index) {
    if (index < 0 || index >= actions.size()) {
      return;
    }
    actions.remove(index);
    adapter.notifyItemRemoved(index);
    persistActions();
  }

  private void showActionPicker(int editIndex) {
    List<String> actionKeys = GestureShortcutMapping.getAllActionKeys(this);
    List<String> filteredKeys = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    String unassigned = getString(R.string.shortcut_value_unassigned);
    String macro1 = getString(R.string.shortcut_value_macro_1);
    String macro2 = getString(R.string.shortcut_value_macro_2);
    String macro3 = getString(R.string.shortcut_value_macro_3);
    for (String key : actionKeys) {
      if (TextUtils.isEmpty(key) || TextUtils.equals(key, unassigned)) {
        continue;
      }
      if (TextUtils.equals(key, macro1) || TextUtils.equals(key, macro2) || TextUtils.equals(key, macro3)) {
        continue;
      }
      filteredKeys.add(key);
      labels.add(GestureShortcutMapping.getActionString(this, key));
    }
    String[] items = labels.toArray(new String[0]);
    new AlertDialog.Builder(this)
        .setTitle(R.string.macro_action_picker_title)
        .setItems(
            items,
            (dialog, which) -> {
              String selected = filteredKeys.get(which);
              if (editIndex >= 0 && editIndex < actions.size()) {
                actions.set(editIndex, selected);
                adapter.notifyItemChanged(editIndex);
              } else {
                actions.add(selected);
                adapter.notifyItemInserted(actions.size() - 1);
              }
              persistActions();
            })
        .show();
  }

  static String getMacroName(Context context, int macroIndex, SharedPreferences prefs) {
    int keyResId;
    int defaultResId;
    switch (macroIndex) {
      case 1:
        keyResId = R.string.pref_macro_1_name_key;
        defaultResId = R.string.macro_default_name_1;
        break;
      case 2:
        keyResId = R.string.pref_macro_2_name_key;
        defaultResId = R.string.macro_default_name_2;
        break;
      case 3:
      default:
        keyResId = R.string.pref_macro_3_name_key;
        defaultResId = R.string.macro_default_name_3;
        break;
    }
    String value = prefs.getString(context.getString(keyResId), null);
    if (TextUtils.isEmpty(value)) {
      value = context.getString(defaultResId);
    }
    return value;
  }

  private static final class MacroActionAdapter extends RecyclerView.Adapter<MacroActionViewHolder> {
    private final Context context;
    private final List<String> actions;
    private final ActionClick editClick;
    private final ActionClick upClick;
    private final ActionClick downClick;
    private final ActionClick removeClick;

    MacroActionAdapter(
        Context context,
        List<String> actions,
        ActionClick editClick,
        ActionClick upClick,
        ActionClick downClick,
        ActionClick removeClick) {
      this.context = context;
      this.actions = actions;
      this.editClick = editClick;
      this.upClick = upClick;
      this.downClick = downClick;
      this.removeClick = removeClick;
    }

    @Override
    public MacroActionViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
      View view =
          android.view.LayoutInflater.from(parent.getContext())
              .inflate(R.layout.item_macro_action, parent, false);
      return new MacroActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MacroActionViewHolder holder, int position) {
      String actionKey = actions.get(position);
      holder.label.setText(GestureShortcutMapping.getActionString(context, actionKey));
      holder.label.setOnClickListener(v -> editClick.onAction(position));
      holder.up.setOnClickListener(v -> upClick.onAction(position));
      holder.down.setOnClickListener(v -> downClick.onAction(position));
      holder.remove.setOnClickListener(v -> removeClick.onAction(position));
    }

    @Override
    public int getItemCount() {
      return actions.size();
    }
  }

  private static final class MacroActionViewHolder extends RecyclerView.ViewHolder {
    final TextView label;
    final Button up;
    final Button down;
    final Button remove;

    MacroActionViewHolder(View itemView) {
      super(itemView);
      label = itemView.findViewById(R.id.macro_action_label);
      up = itemView.findViewById(R.id.macro_action_up);
      down = itemView.findViewById(R.id.macro_action_down);
      remove = itemView.findViewById(R.id.macro_action_remove);
    }
  }

  private interface ActionClick {
    void onAction(int position);
  }
}
