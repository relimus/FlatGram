/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * File created on 24/03/2018
 */
package io.relimus.flatgram.navigation;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import io.relimus.flatgram.ui.SettingsAdapter;
import io.relimus.flatgram.widget.PopupLayout;

public class SettingsWrap {
  public interface OnActionButtonClick {
    boolean onActionButtonClick (SettingsWrap wrap, View view, boolean isCancel);
  }

  public SettingsAdapter adapter;
  public RecyclerView recyclerView;
  public TextView doneButton;
  public TextView cancelButton;
  public PopupLayout window;
}
