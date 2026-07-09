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
package io.relimus.flatgram.component.popups;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.drinkless.tdlib.TdApi;
import io.relimus.flatgram.R;
import io.relimus.flatgram.component.attach.MediaBottomBaseController;
import io.relimus.flatgram.component.attach.MediaLayout;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.navigation.BackHeaderButton;
import io.relimus.flatgram.navigation.HeaderView;
import io.relimus.flatgram.navigation.Menu;
import io.relimus.flatgram.support.ViewSupport;
import io.relimus.flatgram.theme.ColorId;
import io.relimus.flatgram.tool.Strings;

import me.vkryl.android.widget.FrameLayoutFix;

public class JoinRequestsController extends MediaBottomBaseController<Void> implements View.OnClickListener, Menu {
  private boolean allowExpand;

  private final TdApi.ChatJoinRequestsInfo requestsInfo;
  private final JoinRequestsComponent component;
  private int reqCount;

  protected JoinRequestsController (MediaLayout context, long chatId, TdApi.ChatJoinRequestsInfo requestsInfo) {
    super(context, Lang.plural(R.string.xJoinRequests, requestsInfo.totalCount));
    this.component = new JoinRequestsComponent(this, chatId, null);
    this.requestsInfo = requestsInfo;
    this.reqCount = requestsInfo.totalCount;
  }

  @Override
  public void onClick (View v) {
    component.onClick(v);
  }

  @Override
  protected View onCreateView (Context context) {
    buildContentView(false);

    this.component.onCreateView(context, recyclerView);
    ViewSupport.setThemedBackground(recyclerView, ColorId.background);

    initMetrics();
    this.allowExpand = getInitialContentHeight() == super.getInitialContentHeight();

    if (!allowExpand) {
      FrameLayoutFix.LayoutParams params = (FrameLayoutFix.LayoutParams) recyclerView.getLayoutParams();
      params.height = getInitialContentHeight();
      recyclerView.setLayoutParams(params);
    }

    return contentView;
  }

  public void close () {
    mediaLayout.hide(false);
  }

  public void onRequestDecided () {
    reqCount--;

    if (!mediaLayout.getHeaderView().inSearchMode()) {
      setName(Lang.plural(R.string.xJoinRequests, reqCount));
    }

    if (reqCount == 0) {
      close();
    }
  }

  @Override
  protected int getInitialContentHeight () {
    if (requestsInfo != null && requestsInfo.totalCount > 0) {
      return Math.min(super.getInitialContentHeight(), component.getHeight(requestsInfo.totalCount));
    }

    return super.getInitialContentHeight();
  }

  @Override
  protected boolean canExpandHeight () {
    return allowExpand;
  }

  @Override
  public int getId () {
    return R.id.controller_chatJoinRequests;
  }

  @Override
  protected ViewGroup createCustomBottomBar () {
    return new FrameLayout(context);
  }

  @Override
  protected int getBackButton () {
    return BackHeaderButton.TYPE_CLOSE;
  }

  @Override
  public boolean performOnBackPressed (boolean fromTop, boolean commit) {
    if (mediaLayout.getHeaderView().inSearchMode()) {
      if (commit) {
        mediaLayout.getHeaderView().closeSearchMode(true, null);
        headerView = mediaLayout.getHeaderView();
      }
      return true;
    }

    if (!mediaLayout.isHidden()) {
      if (commit) {
        close();
      }
      return true;
    }

    return super.performOnBackPressed(fromTop, commit);
  }

  @Override
  public void destroy () {
    super.destroy();
    component.destroy();
  }

  // Search

  @Override
  protected int getMenuId () {
    return R.id.menu_search;
  }

  @Override
  protected int getSearchMenuId () {
    return R.id.menu_clear;
  }

  @Override
  public void fillMenuItems (int id, HeaderView header, LinearLayout menu) {
    if (id == R.id.menu_search) {
      header.addSearchButton(menu, this);
    } else if (id == R.id.menu_clear) {
      header.addClearButton(menu, this);
    }
  }

  @Override
  public void onMenuItemPressed (int id, View view) {
    if (id == R.id.menu_btn_search) {
      mediaLayout.getHeaderView().openSearchMode();
      headerView = mediaLayout.getHeaderView();
    } else if (id == R.id.menu_btn_clear) {
      clearSearchInput();
    }
  }

  @Override
  protected void onLeaveSearchMode () {
    component.search(null);
  }

  @Override
  protected void onAfterLeaveSearchMode () {
    runOnUiThread(() -> setName(Lang.plural(R.string.xJoinRequests, reqCount)), 100);
  }

  @Override
  protected void onSearchInputChanged (final String query) {
    component.search(Strings.clean(query.trim()));
  }
}
