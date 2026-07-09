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
package io.relimus.flatgram.widget;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import io.relimus.flatgram.charts.LayoutHelper;
import io.relimus.flatgram.component.sticker.StickerSmallView;
import io.relimus.flatgram.component.sticker.TGStickerObj;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.theme.Theme;
import io.relimus.flatgram.theme.ThemeInvalidateListener;

import me.vkryl.core.lambda.Destroyable;

public class EmbeddableStickerView extends LinearLayout implements ThemeInvalidateListener, Destroyable {
  private final StickerSmallView stickerSmallView;
  private final TextView captionTextView;

  public EmbeddableStickerView (Context context) {
    this(context, null, 0);
  }

  public EmbeddableStickerView (Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public EmbeddableStickerView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setOrientation(LinearLayout.VERTICAL);

    stickerSmallView = new StickerSmallView(context) {
      @Override
      public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
      }
    };
    stickerSmallView.setLayoutParams(LayoutHelper.createLinear(128, 128, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));
    addView(stickerSmallView);

    captionTextView = new TextView(context);
    captionTextView.setGravity(Gravity.CENTER_HORIZONTAL);
    captionTextView.setTextSize(14);
    captionTextView.setMovementMethod(LinkMovementMethod.getInstance());
    captionTextView.setHighlightColor(Theme.textLinkHighlightColor());
    addView(captionTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 16, 16, 8, 16, 8));

    recolor();
  }

  public void recolor () {
    captionTextView.setTextColor(Theme.textDecentColor());
    captionTextView.setHighlightColor(Theme.textLinkHighlightColor());
  }

  public void attach () {
    stickerSmallView.attach();
  }

  public void detach () {
    stickerSmallView.detach();
  }

  public void setCaptionText (CharSequence text) {
    captionTextView.setText(text);
  }

  public void init (Tdlib tdlib) {
    stickerSmallView.init(tdlib);
  }

  public void setSticker (TGStickerObj tgStickerObj) {
    if (tgStickerObj != null && !tgStickerObj.isEmpty()) {
      tgStickerObj.getPreviewAnimation().setPlayOnce(false);
    }

    stickerSmallView.setSticker(tgStickerObj);
  }

  @Override
  public void performDestroy () {
    stickerSmallView.performDestroy();
  }

  @Override
  public void onThemeInvalidate (boolean isTempUpdate) {
    recolor();
    invalidate();
  }
}
