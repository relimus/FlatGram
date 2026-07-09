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
 * File created on 26/01/2017
 */
package io.relimus.flatgram.support;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.NonNull;

import io.relimus.flatgram.tool.Paints;
import io.relimus.flatgram.tool.Screen;

public class CircleDrawable extends SimpleShapeDrawable {
  public CircleDrawable (int colorId, float size, boolean isPressed) {
    super(colorId, size, isPressed);
  }

  @Override
  public void draw (@NonNull Canvas c) {
    Rect rect = getBounds();
    int centerX = rect.centerX();
    int centerY = rect.centerY();
    int radius = Screen.dp(size) / 2;

    final int color = getDrawColor();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (color != 0) {
        c.drawCircle(centerX, centerY, radius, Paints.fillingPaint(color));
      }
    } else {
      c.drawCircle(centerX, centerY, radius, Paints.shadowFillingPaint(color));
    }
  }
}
