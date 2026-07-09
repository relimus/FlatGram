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
 * File created on 03/12/2016
 */
package io.relimus.flatgram.data;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import io.relimus.flatgram.BaseActivity;
import io.relimus.flatgram.loader.ImageFile;
import io.relimus.flatgram.loader.ImageFileLocal;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.tool.Screen;

public class InlineResultPhoto extends InlineResult<TdApi.InlineQueryResultPhoto> {
  private @Nullable ImageFile miniThumbnail, preview;
  private @Nullable ImageFile image;
  private final int width, height;

  public InlineResultPhoto (BaseActivity context, Tdlib tdlib, TdApi.InlineQueryResultPhoto data) {
    super(context, tdlib, TYPE_PHOTO, data.id, data);

    if (data.photo.minithumbnail != null) {
      this.miniThumbnail = new ImageFileLocal(data.photo.minithumbnail);
      this.miniThumbnail.setScaleType(ImageFile.CENTER_CROP);
    }

    final TdApi.PhotoSize previewSize = MediaWrapper.buildPreviewSize(data.photo);
    final TdApi.PhotoSize targetSize = MediaWrapper.buildTargetFile(data.photo, previewSize);

    this.width = MediaWrapper.getWidth(data.photo, targetSize);
    this.height = MediaWrapper.getHeight(data.photo, targetSize);

    if (previewSize != null) {
      this.preview = new ImageFile(tdlib, previewSize.photo);
      this.preview.setScaleType(ImageFile.CENTER_CROP);
      this.preview.setNeedCancellation(false);
    }

    if (targetSize != null) {
      this.image = new ImageFile(tdlib, targetSize.photo);
      this.image.setScaleType(ImageFile.CENTER_CROP);
      this.image.setNeedCancellation(false);
      this.image.setSize(Screen.dp(128f));
    }
  }

  @Override
  public int getCellWidth () {
    return width;
  }

  @Override
  public int getCellHeight () {
    return height;
  }

  @Nullable
  public ImageFile getMiniThumbnail () {
    return miniThumbnail;
  }

  @Nullable
  public ImageFile getPreview () {
    return preview;
  }

  @Nullable
  public ImageFile getImage () {
    return image;
  }
}
