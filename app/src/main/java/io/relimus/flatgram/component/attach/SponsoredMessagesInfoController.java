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
 */
package io.relimus.flatgram.component.attach;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.relimus.flatgram.R;
import io.relimus.flatgram.component.base.SettingView;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.navigation.BackHeaderButton;
import io.relimus.flatgram.support.ViewSupport;
import io.relimus.flatgram.theme.ColorId;
import io.relimus.flatgram.tool.Intents;
import io.relimus.flatgram.ui.ListItem;
import io.relimus.flatgram.ui.SettingsAdapter;

public class SponsoredMessagesInfoController extends MediaBottomBaseController<Void> implements View.OnClickListener {
  private SettingsAdapter adapter;

  public SponsoredMessagesInfoController (MediaLayout context, int titleResource) {
    super(context, titleResource);
  }

  @Override
  protected View onCreateView (Context context) {
    buildContentView(false);
    setLayoutManager(new LinearLayoutManager(context(), RecyclerView.VERTICAL, false));

    setAdapter(adapter = new SettingsAdapter(this) {
      @Override
      protected void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
        if (item.getId() == R.id.btn_openLink) {
          view.setIconColorId(ColorId.textNeutral);
        } else {
          view.setIconColorId(ColorId.icon);
        }
      }
    });

    ViewSupport.setThemedBackground(recyclerView, ColorId.background);

    adapter.setItems(new ListItem[] {
      new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL),
      new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.SponsoredInfoText),
      new ListItem(ListItem.TYPE_SHADOW_TOP),
      new ListItem(ListItem.TYPE_SETTING, R.id.btn_openLink, R.drawable.baseline_language_24, Lang.getString(R.string.url_promote), false).setTextColorId(ColorId.textNeutral),
      new ListItem(ListItem.TYPE_SHADOW_BOTTOM),
      new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.SponsoredInfoText2),
    }, false);

    return contentView;
  }

  @Override
  protected ViewGroup createCustomBottomBar () {
    RecyclerView rv = new RecyclerView(context);
    rv.setLayoutManager(new LinearLayoutManager(context));
    SettingsAdapter sa = new SettingsAdapter(this) {
      @Override
      protected void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
        if (item.getId() == R.id.btn_close) {
          view.setIconColorId(ColorId.textNeutral);
        } else {
          view.setIconColorId(ColorId.icon);
        }
      }
    };
    rv.setAdapter(sa);
    sa.setItems(new ListItem[] {
      new ListItem(ListItem.TYPE_SHADOW_TOP),
      new ListItem(ListItem.TYPE_SETTING, R.id.btn_close, R.drawable.baseline_check_circle_24, R.string.Continue).setTextColorId(ColorId.textNeutral),
      new ListItem(ListItem.TYPE_SETTING, R.id.btn_openLink, R.drawable.baseline_help_24, R.string.SponsoredInfoAction),
    }, false);
    return rv;
  }

  @Override
  public int getId () {
    return R.id.controller_sponsoredMessagesInfo;
  }

  @Override
  public void onClick (View v) {
    if (v.getId() == R.id.btn_openLink) {
      mediaLayout.hide(false);
      Intents.openUriInBrowser(Uri.parse(Lang.getString(R.string.url_promote)));
    }
  }

  @Override
  protected int getBackButton () {
    return BackHeaderButton.TYPE_CLOSE;
  }

  @Override
  public boolean performOnBackPressed (boolean fromTop, boolean commit) {
    if (!mediaLayout.isHidden()) {
      if (commit) {
        mediaLayout.hide(false);
      }
      return true;
    }
    return super.performOnBackPressed(fromTop, commit);
  }
}
