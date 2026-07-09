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
 * File created on 19/01/2024
 */
package io.relimus.flatgram.util;

import io.relimus.flatgram.R;
import io.relimus.flatgram.theme.ColorId;

public class PremiumLockModifier extends EndIconModifier {
  public PremiumLockModifier () {
    super(R.drawable.baseline_lock_16, ColorId.text);
  }
}
