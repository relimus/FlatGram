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
 * File created on 05/06/2023
 */
package io.relimus.flatgram.component.emoji;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.relimus.flatgram.R;
import io.relimus.flatgram.component.sticker.TGStickerObj;
import io.relimus.flatgram.config.Config;
import io.relimus.flatgram.loader.ImageFile;
import io.relimus.flatgram.loader.ImageReceiver;
import io.relimus.flatgram.loader.gif.GifFile;
import io.relimus.flatgram.loader.gif.GifReceiver;
import io.relimus.flatgram.theme.PorterDuffColorId;
import io.relimus.flatgram.tool.Drawables;

import me.vkryl.core.lambda.Destroyable;

public class AnimatedEmojiDrawable extends Drawable implements Destroyable {

  private final ImageReceiver imageReceiver;
  private final GifReceiver gifReceiver;
  private Drawable drawable;

  public AnimatedEmojiDrawable (View parentView) {
    imageReceiver = new ImageReceiver(parentView, 0);
    gifReceiver = new GifReceiver(parentView);
  }

  public void setSticker (TGStickerObj sticker, boolean isPlayOnce) {
    if (sticker.isDefaultPremiumStar()) {
      drawable = Drawables.get(R.drawable.baseline_premium_star_28).mutate();
      return;
    }
    ImageFile imageFile = sticker.getImage();
    GifFile animation = sticker.getPreviewAnimation();
    if (animation != null) {
      if (isPlayOnce) {
        animation.setPlayOnce(true);
        animation.setLooped(false);
      }
      gifReceiver.requestFile(animation);
    }
    imageReceiver.requestFile(imageFile);
  }

  public void attach () {
    imageReceiver.attach();
    gifReceiver.attach();
  }

  public void detach () {
    imageReceiver.detach();
    gifReceiver.detach();
  }

  @Override
  public void draw (@NonNull Canvas canvas) {
    if (drawable != null) {
      drawable.draw(canvas);
      return;
    }
    if (gifReceiver.needPlaceholder() || Config.DEBUG_REACTIONS_ANIMATIONS) {
      imageReceiver.draw(canvas);
    }
    gifReceiver.draw(canvas);
  }

  @Override
  public void setBounds (@NonNull Rect bounds) {
    gifReceiver.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    imageReceiver.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    if (drawable != null) {
      drawable.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }
    super.setBounds(bounds);
  }

  @Override
  public void setBounds (int left, int top, int right, int bottom) {
    gifReceiver.setBounds(left, top, right, bottom);
    imageReceiver.setBounds(left, top, right, bottom);
    if (drawable != null) {
      drawable.setBounds(left, top, right, bottom);
    }
    super.setBounds(left, top, right, bottom);
  }

  @Override
  public void setAlpha (int i) {
    gifReceiver.setAlpha(i / 255f);
    imageReceiver.setAlpha(i / 255f);
    if (drawable != null) {
      drawable.setAlpha(i);
    }
  }

  public void setThemedPorterDuffColorId (@PorterDuffColorId int colorId) {
    gifReceiver.setThemedPorterDuffColorId(colorId);
    imageReceiver.setThemedPorterDuffColorId(colorId);
    if (drawable != null) {
      drawable.setColorFilter(imageReceiver.getBitmapPaint().getColorFilter());
    }
  }
  public void setPorterDuffColorFilter (@ColorInt int color) {
    gifReceiver.setPorterDuffColorFilter(color);
    imageReceiver.setPorterDuffColorFilter(color);
    if (drawable != null) {
      drawable.setColorFilter(imageReceiver.getBitmapPaint().getColorFilter());
    }
  }
  public void disablePorterDuffColorFilter () {
    gifReceiver.disablePorterDuffColorFilter();
    imageReceiver.disablePorterDuffColorFilter();
    if (drawable != null) {
      drawable.setColorFilter(null);
    }
  }

  @Override
  public void setColorFilter (@Nullable ColorFilter colorFilter) {

  }

  @Override
  @SuppressWarnings("deprecation")
  public int getOpacity () {
    return PixelFormat.UNKNOWN;
  }

  @Override
  public void performDestroy () {
    gifReceiver.destroy();
    imageReceiver.destroy();
  }
}
