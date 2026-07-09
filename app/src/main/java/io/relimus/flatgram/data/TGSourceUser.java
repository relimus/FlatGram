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
 * File created on 20/02/2016 at 18:26
 */
package io.relimus.flatgram.data;

import android.view.View;

import org.drinkless.tdlib.TdApi;
import io.relimus.flatgram.R;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.loader.AvatarReceiver;
import io.relimus.flatgram.loader.Receiver;
import io.relimus.flatgram.telegram.TdlibAccentColor;
import io.relimus.flatgram.telegram.TdlibCache;
import io.relimus.flatgram.telegram.TdlibUi;
import io.relimus.flatgram.util.text.Text;
import io.relimus.flatgram.util.text.TextPart;

public class TGSourceUser extends TGSource implements TdlibCache.UserDataChangeListener {
  private final long senderUserId;
  private TdApi.User user;

  public TGSourceUser (TGMessage msg, TdApi.MessageOriginUser info) {
    super(msg);
    this.senderUserId = info.senderUserId;
  }

  public TdApi.User getUser () {
    return user;
  }

  @Override
  public void load () {
    TdApi.User user = msg.tdlib().cache().user(senderUserId);
    msg.tdlib().cache().addUserDataListener(senderUserId, this);
    if (user != null) {
      this.user = user;
      this.isReady = true;
      msg.rebuildForward();
    }
  }

  @Override
  public boolean open (View view, Text text, TextPart part, TdlibUi.UrlOpenParameters openParameters, Receiver receiver) {
    if (user != null) {
      msg.tdlib().ui().openPrivateProfile(msg.controller(), user.id, openParameters);
      return true;
    }
    return false;
  }

  @Override
  public void destroy () {
    msg.tdlib().cache().removeUserDataListener(senderUserId, this);
  }

  @Override
  public void onUserUpdated (TdApi.User user) {
    this.user = user;
    msg.runOnUiThreadOptional(() -> {
      msg.rebuildForward();
      msg.postInvalidate();
    });
  }

  public long getSenderUserId () {
    return senderUserId;
  }

  @Override
  public String getAuthorName () {
    return user == null ? Lang.getString(R.string.LoadingUser) : TD.getUserName(user);
  }

  @Override
  public TdlibAccentColor getAuthorAccentColor () {
    return msg.tdlib.cache().userAccentColor(senderUserId);
  }

  @Override
  public void requestAvatar (AvatarReceiver receiver) {
    receiver.requestUser(msg.tdlib, senderUserId, AvatarReceiver.Options.NONE);
  }
}
