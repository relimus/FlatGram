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
 * File created on 29/03/2019
 */
package io.relimus.flatgram.data;

import android.view.View;

import org.drinkless.tdlib.TdApi;
import io.relimus.flatgram.R;
import io.relimus.flatgram.loader.AvatarReceiver;
import io.relimus.flatgram.loader.Receiver;
import io.relimus.flatgram.telegram.TdlibAccentColor;
import io.relimus.flatgram.telegram.TdlibUi;
import io.relimus.flatgram.util.text.Text;
import io.relimus.flatgram.util.text.TextPart;

public class TGSourceHidden extends TGSource {
  private final String name;
  private final TdlibAccentColor accentColor;
  private final boolean isImported;

  public TGSourceHidden (TGMessage msg, TdApi.MessageOriginHiddenUser forward) {
    this(msg, forward.senderName, false);
  }

  public TGSourceHidden (TGMessage msg, TdApi.MessageImportInfo messageImport) {
    this(msg, messageImport.senderName, true);
  }

  private TGSourceHidden (TGMessage msg, String name, boolean isImported) {
    super(msg);
    this.name = name;
    this.accentColor = msg.tdlib.accentColorForString(name);
    this.isImported = isImported;
    this.isReady = true;
  }

  @Override
  public boolean open (View view, Text text, TextPart part, TdlibUi.UrlOpenParameters parameters, Receiver receiver) {
    msg.context()
      .tooltipManager()
      .builder(view, msg.currentViews)
      .locate(text != null ? (targetView, outRect) -> text.locatePart(outRect, part) : receiver != null ? (targetView, outRect) -> receiver.toRect(outRect) : null)
      .controller(msg.controller())
      .show(msg.tdlib(), msg.isImported() ? R.string.ForwardAuthorImported : R.string.ForwardAuthorHidden)
      .hideDelayed();
    return true;
  }

  @Override
  public void load () { }

  @Override
  public String getAuthorName () {
    return name;
  }

  @Override
  public TdlibAccentColor getAuthorAccentColor () {
    if (isImported) {
      return accentColor;
    } else {
      return null;
    }
  }

  @Override
  public void requestAvatar (AvatarReceiver receiver) {
    receiver.requestPlaceholder(msg.tdlib,
      new AvatarPlaceholder.Metadata(
        accentColor,
        isImported ? null : TD.getLetters(name),
        isImported ? R.drawable.baseline_phone_24 : 0, 0
      ), AvatarReceiver.Options.NONE
    );
  }

  @Override
  public void destroy () { }
}
