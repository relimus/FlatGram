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
 * File created on 08/01/2017
 */
package io.relimus.flatgram.data;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import io.relimus.flatgram.BaseActivity;
import io.relimus.flatgram.N;
import io.relimus.flatgram.U;
import io.relimus.flatgram.component.inline.CustomResultView;
import io.relimus.flatgram.emoji.Emoji;
import io.relimus.flatgram.loader.ComplexReceiver;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.theme.ColorId;
import io.relimus.flatgram.theme.Theme;
import io.relimus.flatgram.theme.ThemeId;
import io.relimus.flatgram.tool.Paints;
import io.relimus.flatgram.tool.Screen;
import io.relimus.flatgram.tool.Strings;

import me.vkryl.core.StringUtils;

public class InlineResultEmojiSuggestion extends InlineResult<N.Suggestion> {
  public static final char[] SPECIAL_SPLITTERS = {'_', '{', '}', '/', '(', ')', ':', ';'};

  private CharSequence text;
  private CharSequence emoji;
  private float textWidth;

  public InlineResultEmojiSuggestion (BaseActivity context, Tdlib tdlib, N.Suggestion suggestion, @Nullable String query) {
    super(context, tdlib, TYPE_EMOJI_SUGGESTION, null, suggestion);
    this.emoji = Emoji.instance().replaceEmoji(suggestion.emoji);
    this.text = Strings.highlightWords(suggestion.label, query, suggestion.label.startsWith(":") ? 1 : 0, SPECIAL_SPLITTERS);
    this.textWidth = U.measureText(suggestion.label, Paints.getTextPaint15());
  }

  @Override
  public void setForceDarkMode (boolean forceDarkMode) {
    super.setForceDarkMode(forceDarkMode);
    Strings.forceHighlightSpansThemeId(text, forceDarkMode ? ThemeId.NIGHT_BLACK : ThemeId.NONE);
  }

  public String getEmoji () {
    return data.emoji;
  }

  private Layout emojiLayout;

  private CharSequence trimmedText;
  private Layout textLayout;

  @Override
  protected void layoutInternal (int contentWidth) {
    int availWidth = contentWidth - Screen.dp(12f) - Screen.dp(55f) - Screen.dp(24f);
    trimmedText = StringUtils.isEmpty(text) ? text : textWidth > availWidth ? TextUtils.ellipsize(text, Paints.getTextPaint15(), availWidth, TextUtils.TruncateAt.END) : text;
    if (StringUtils.isEmpty(trimmedText) || trimmedText instanceof String) {
      textLayout = null;
    } else {
      textLayout = U.createLayout(trimmedText, availWidth, Paints.getTextPaint15());
    }
    emojiLayout = U.createLayout(emoji, availWidth, Paints.getTitlePaint(false));
  }

  @Override
  protected int getContentHeight () {
    return Screen.dp(4f) * 2 + Screen.dp(14f) * 2;
  }

  @Override
  protected void drawInternal (CustomResultView view, Canvas c, ComplexReceiver receiver, int viewWidth, int viewHeight, int startY) {
    int startX = Screen.dp(55f);
    startY += Screen.dp(4f) + Screen.dp(14f) + Screen.dp(5f);
    if (emojiLayout != null) {
      c.save();
      c.translate(startX, startY - Screen.dp(13f));
      emojiLayout.draw(c);
      c.restore();
      startX += Screen.dp(24f);
    }
    if (trimmedText != null) {
      final int color = forceDarkMode ? Theme.getColor(ColorId.text, ThemeId.NIGHT_BLACK) : Theme.textAccentColor();
      if (textLayout != null) {
        c.save();
        c.translate(startX, startY - Screen.dp(13f));
        Paint paint = Paints.getTextPaint15(color);
        textLayout.draw(c);
        paint.setColor(color);
        c.restore();
      } else if (!StringUtils.isEmpty(trimmedText)) {
        c.drawText((String) trimmedText, startX, startY, Paints.getTextPaint15(color));
      }
    }
  }
}
