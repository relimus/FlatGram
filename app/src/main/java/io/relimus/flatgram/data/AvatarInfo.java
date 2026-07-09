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
 * File created on 14/12/2017
 */
package io.relimus.flatgram.data;

import org.drinkless.tdlib.TdApi;
import io.relimus.flatgram.loader.ImageFile;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.telegram.TdlibAccentColor;
import io.relimus.flatgram.tool.Paints;
import io.relimus.flatgram.util.text.Letters;

public class AvatarInfo {
  public final Tdlib tdlib;
  public final long userId;
  public ImageFile imageFile;

  public TdlibAccentColor accentColor;
  public Letters letters;

  public float lettersWidth15dp;

  public AvatarInfo (Tdlib tdlib, long userId) {
    this.tdlib = tdlib;
    this.userId = userId;
    updateUser();
  }

  public void updateUser () {
    TdApi.User user = tdlib.cache().user(userId);
    letters = TD.getLetters(user);
    accentColor = tdlib.cache().userAccentColor(user);
    imageFile = TD.getAvatar(tdlib, user);
    lettersWidth15dp = Paints.measureLetters(letters, 15f);
  }


}
