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

import io.relimus.flatgram.R;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.v.CustomRecyclerView;

public class SettingsFlatGramInterfaceController extends RecyclerViewController<Void> {
  public SettingsFlatGramInterfaceController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_flatGramInterfaceSettings;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.TweakSettings);
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    SettingsAdapter adapter = new SettingsAdapter(this);
    adapter.setItems(new ListItem[] {
      new ListItem(ListItem.TYPE_EMPTY, 0, 0, R.string.FlatGramInterfaceSettingsEmpty)
    }, false);
    recyclerView.setAdapter(adapter);
  }
}
