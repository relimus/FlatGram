/*
 * This file is a part of FlatGram by relimus
 * Copyright © 2026 (relimus@proton.me)
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
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import io.relimus.flatgram.R;
import io.relimus.flatgram.component.dialogs.DownloadedFileItem;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.loader.DoubleImageReceiver;
import io.relimus.flatgram.navigation.ViewController;
import io.relimus.flatgram.theme.ColorId;
import io.relimus.flatgram.theme.Theme;
import io.relimus.flatgram.tool.Fonts;
import io.relimus.flatgram.tool.Paints;
import io.relimus.flatgram.tool.Screen;

public class DownloadedFileView extends FrameLayout {
  private final ImageView iconView;
  private final ImageView stateIconView;
  private final TextView titleView;
  private final TextView subtitleView;
  private final DoubleImageReceiver previewReceiver;
  private @Nullable DownloadedFileItem item;
  private boolean selected;
  private boolean suppressClickFeedback;

  public DownloadedFileView (Context context) {
    super(context);

    setLayoutParams(new RecyclerView.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(72f)
    ));
    setWillNotDraw(false);

    iconView = new ImageView(context);
    iconView.setScaleType(ImageView.ScaleType.CENTER);
    iconView.setColorFilter(Theme.getColor(ColorId.icon));
    iconView.setImageResource(R.drawable.baseline_insert_drive_file_24);
    addView(iconView);

    previewReceiver = new DoubleImageReceiver(this, Screen.dp(4f));

    stateIconView = new ImageView(context);
    stateIconView.setScaleType(ImageView.ScaleType.CENTER);
    stateIconView.setColorFilter(Theme.getColor(ColorId.textLight));
    addView(stateIconView);

    titleView = new EmojiTextView(context);
    titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f);
    titleView.setTypeface(Fonts.getRobotoMedium());
    titleView.setTextColor(Theme.textAccentColor());
    titleView.setSingleLine(true);
    titleView.setEllipsize(TextUtils.TruncateAt.END);
    titleView.setGravity(Lang.gravity());
    addView(titleView);

    subtitleView = new EmojiTextView(context);
    subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
    subtitleView.setTypeface(Fonts.getRobotoRegular());
    subtitleView.setTextColor(Theme.textDecentColor());
    subtitleView.setSingleLine(true);
    subtitleView.setEllipsize(TextUtils.TruncateAt.END);
    subtitleView.setGravity(Lang.gravity());
    addView(subtitleView);
  }

  public void addThemeListeners (@Nullable ViewController<?> themeProvider) {
    if (themeProvider != null) {
      themeProvider.addThemeFilterListener(iconView, ColorId.icon);
      themeProvider.addThemeFilterListener(stateIconView, ColorId.textLight);
      themeProvider.addThemeTextAccentColorListener(titleView);
      themeProvider.addThemeTextDecentColorListener(subtitleView);
      themeProvider.addThemeInvalidateListener(this);
    }
  }

  public void setItem (@Nullable DownloadedFileItem item, boolean selected,
                       boolean suppressClickFeedback) {
    DownloadedFileItem oldItem = this.item;
    this.item = item;
    this.selected = selected;
    this.suppressClickFeedback = suppressClickFeedback;
    if (suppressClickFeedback) {
      setPressed(false);
    }
    if (oldItem == null || item == null || !TextUtils.equals(oldItem.title, item.title)) {
      titleView.setText(item != null ? item.title : null);
    }
    if (oldItem == null || item == null || !TextUtils.equals(oldItem.subtitle, item.subtitle)) {
      subtitleView.setText(item != null ? item.subtitle : null);
    }
    if (hasStateIcon()) {
      stateIconView.setVisibility(View.VISIBLE);
      if (oldItem == null || !oldItem.isActive() || oldItem.paused != item.paused) {
        stateIconView.setImageResource(
          item.paused ? R.drawable.baseline_download_14 : R.drawable.baseline_pause_14
        );
      }
    } else {
      stateIconView.setVisibility(View.GONE);
    }
    iconView.setAlpha(item != null && item.completed ? 0.72f : 1f);
    if (hasPreview()) {
      iconView.setVisibility(View.INVISIBLE);
      if (oldItem == null || oldItem.miniThumbnail != item.miniThumbnail ||
          oldItem.preview != item.preview) {
        previewReceiver.requestFile(item.miniThumbnail, item.preview);
      }
    } else {
      iconView.setVisibility(View.VISIBLE);
      if (oldItem != null && (oldItem.miniThumbnail != null || oldItem.preview != null)) {
        previewReceiver.clear();
      }
    }
    invalidate();
  }

  @Override
  public void setPressed (boolean pressed) {
    super.setPressed(suppressClickFeedback ? false : pressed);
  }

  @Override
  protected void onAttachedToWindow () {
    super.onAttachedToWindow();
    previewReceiver.attach();
  }

  @Override
  protected void onDetachedFromWindow () {
    previewReceiver.detach();
    super.onDetachedFromWindow();
  }

  @Override
  protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int width = getMeasuredWidth();
    int iconSize = Screen.dp(48f);
    iconView.measure(
      MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY)
    );
    stateIconView.measure(
      MeasureSpec.makeMeasureSpec(Screen.dp(12f), MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(Screen.dp(12f), MeasureSpec.EXACTLY)
    );

    int textLeft = Lang.rtl() ? Screen.dp(16f) : Screen.dp(72f);
    int textRight = Lang.rtl() ? width - Screen.dp(72f) : width - Screen.dp(16f);
    int textWidth = Math.max(0, textRight - textLeft);
    titleView.measure(
      MeasureSpec.makeMeasureSpec(textWidth, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(Screen.dp(22f), MeasureSpec.AT_MOST)
    );
    subtitleView.measure(
      MeasureSpec.makeMeasureSpec(
        Math.max(0, textWidth - (hasStateIcon() ? Screen.dp(18f) : 0)), MeasureSpec.EXACTLY
      ),
      MeasureSpec.makeMeasureSpec(Screen.dp(20f), MeasureSpec.AT_MOST)
    );

  }

  @Override
  protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
    int width = right - left;
    int iconSize = Screen.dp(48f);
    int iconLeft = Lang.rtl() ? width - Screen.dp(12f) - iconSize : Screen.dp(12f);
    iconView.layout(iconLeft, Screen.dp(12f), iconLeft + iconSize, Screen.dp(60f));
    previewReceiver.setBounds(iconLeft, Screen.dp(12f), iconLeft + iconSize, Screen.dp(60f));

    int textLeft = Lang.rtl() ? Screen.dp(16f) : Screen.dp(72f);
    int textRight = Lang.rtl() ? width - Screen.dp(72f) : width - Screen.dp(16f);
    titleView.layout(textLeft, Screen.dp(15f), textRight, Screen.dp(37f));
    if (hasStateIcon() && Lang.rtl()) {
      stateIconView.layout(textRight - Screen.dp(12f), Screen.dp(42f), textRight, Screen.dp(54f));
      subtitleView.layout(textLeft, Screen.dp(38f), textRight - Screen.dp(18f), Screen.dp(58f));
    } else if (hasStateIcon()) {
      stateIconView.layout(textLeft, Screen.dp(42f), textLeft + Screen.dp(12f), Screen.dp(54f));
      subtitleView.layout(textLeft + Screen.dp(18f), Screen.dp(38f), textRight, Screen.dp(58f));
    } else {
      subtitleView.layout(textLeft, Screen.dp(38f), textRight, Screen.dp(58f));
    }
  }

  @Override
  protected void onDraw (Canvas c) {
    if (selected) {
      c.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(),
        Paints.fillingPaint(Theme.chatSelectionColor()));
    }
    if (hasPreview()) {
      if (previewReceiver.needPlaceholder()) {
        previewReceiver.drawPlaceholder(c);
      }
      previewReceiver.draw(c);
    }
    super.onDraw(c);
  }

  private boolean hasPreview () {
    return item != null && (item.miniThumbnail != null || item.preview != null);
  }

  private boolean hasStateIcon () {
    return item != null && item.isActive();
  }
}
