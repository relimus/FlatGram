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
 * File created on 20/02/2016 at 18:09
 */
package io.relimus.flatgram.data;

import android.view.View;

import androidx.annotation.Nullable;

import io.relimus.flatgram.loader.AvatarReceiver;
import io.relimus.flatgram.loader.Receiver;
import io.relimus.flatgram.telegram.TdlibAccentColor;
import io.relimus.flatgram.telegram.TdlibUi;
import io.relimus.flatgram.util.text.Text;
import io.relimus.flatgram.util.text.TextPart;

public abstract class TGSource {
  protected TGMessage msg;
  protected boolean isReady;

  public TGSource (TGMessage msg) {
    this.msg = msg;
  }

  public abstract boolean open (View view, Text text, TextPart part,  @Nullable TdlibUi.UrlOpenParameters openParameters, Receiver receiver);
  public abstract void load ();
  public abstract String getAuthorName ();
  public abstract TdlibAccentColor getAuthorAccentColor ();
  public abstract void requestAvatar (AvatarReceiver receiver);
  public abstract void destroy ();

  public boolean isReady () {
    return isReady;
  }
}
