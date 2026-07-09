/*
 * This file is a part of FlatGram by relimus
 * Copyright © 2026 (relimus@proton.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.relimus.flatgram.ui;

import android.content.Context;
import android.view.View;

import io.relimus.flatgram.R;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.v.CustomRecyclerView;

public class SettingsFlatGramController extends RecyclerViewController<Void> implements
  View.OnClickListener {
  public SettingsFlatGramController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_flatGramSettings;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.AppName);
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    SettingsAdapter adapter = new SettingsAdapter(this);
    adapter.setItems(new ListItem[] {
      new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL),
      new ListItem(ListItem.TYPE_SHADOW_TOP),
      new ListItem(ListItem.TYPE_SETTING, R.id.btn_flatGramInterfaceSettings,
        R.drawable.baseline_extension_24, R.string.FlatGramInterfaceSettings),
      new ListItem(ListItem.TYPE_SHADOW_BOTTOM),
      new ListItem(ListItem.TYPE_BUILD_NO, R.id.btn_build, 0,
        Lang.getAppBuildAndVersion(tdlib), false)
    }, false);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onClick (View v) {
    if (v.getId() == R.id.btn_flatGramInterfaceSettings) {
      navigateTo(new SettingsFlatGramInterfaceController(context, tdlib));
    }
  }
}
