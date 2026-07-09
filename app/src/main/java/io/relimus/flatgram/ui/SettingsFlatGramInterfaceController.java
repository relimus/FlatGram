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
import io.relimus.flatgram.component.base.SettingView;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.unsorted.Settings;
import io.relimus.flatgram.v.CustomRecyclerView;

public class SettingsFlatGramInterfaceController extends RecyclerViewController<Void> implements
  View.OnClickListener {
  private SettingsAdapter adapter;

  public SettingsFlatGramInterfaceController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_flatGramInterfaceSettings;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.FlatGramInterfaceSettings);
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this) {
      @Override
      public void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
        if (item.getId() == R.id.btn_flatGramHomeTopBar) {
          view.setData(Settings.instance().useFlatGramHomeTopBar() ?
            R.string.FlatGramHomeTopBarFlatGram :
            R.string.FlatGramHomeTopBarDefault);
        }
      }
    };
    adapter.setItems(new ListItem[] {
      new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL),
      new ListItem(ListItem.TYPE_SHADOW_TOP),
      new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_flatGramHomeTopBar,
        0, R.string.FlatGramHomeTopBar),
      new ListItem(ListItem.TYPE_SHADOW_BOTTOM)
    }, false);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onClick (View v) {
    if (v.getId() == R.id.btn_flatGramHomeTopBar) {
      showHomeTopBarOptions();
    }
  }

  private void showHomeTopBarOptions () {
    int mode = Settings.instance().getFlatGramHomeTopBarMode();
    showSettings(R.id.btn_flatGramHomeTopBar, new ListItem[] {
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_flatGramHomeTopBarDefault, 0,
        R.string.FlatGramHomeTopBarDefault, R.id.btn_flatGramHomeTopBar,
        mode == Settings.FLATGRAM_HOME_TOP_BAR_MODE_DEFAULT),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_flatGramHomeTopBarFlatGram, 0,
        R.string.FlatGramHomeTopBarFlatGram, R.id.btn_flatGramHomeTopBar,
        mode == Settings.FLATGRAM_HOME_TOP_BAR_MODE_FLATGRAM)
    }, (id, result) -> {
      Settings.instance().setFlatGramHomeTopBarMode(
        result.get(R.id.btn_flatGramHomeTopBar) == R.id.btn_flatGramHomeTopBarFlatGram ?
          Settings.FLATGRAM_HOME_TOP_BAR_MODE_FLATGRAM :
          Settings.FLATGRAM_HOME_TOP_BAR_MODE_DEFAULT);
      adapter.updateValuedSettingById(R.id.btn_flatGramHomeTopBar);
    });
  }
}
