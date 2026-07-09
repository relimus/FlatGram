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
 * File created on 31/08/2022, 22:07.
 */

package io.relimus.flatgram.widget;

import android.content.Context;
import android.os.Build;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import io.relimus.flatgram.emoji.EmojiInputConnection;
import io.relimus.flatgram.emoji.EmojiUpdater;
import io.relimus.flatgram.util.DestroySpansWatcher;
import io.relimus.flatgram.v.EditText;

import me.vkryl.core.lambda.Destroyable;

public class EmojiEditText extends EditText implements Destroyable {
  private EmojiUpdater emojiUpdater;

  public EmojiEditText (Context context) {
    super(context);
    init();
  }

  public EmojiEditText (Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public EmojiEditText (Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init () {
    addTextChangedListener(new DestroySpansWatcher());
    setFilters(new InputFilter[0]);
  }

  @Override
  public final void setFilters (@NonNull InputFilter[] filters) {
    if (emojiUpdater == null)
      emojiUpdater = new EmojiUpdater(this);
    super.setFilters(EmojiTextView.newFilters(this, filters, emojiUpdater));
  }

  @Override
  @CallSuper
  public void performDestroy () {
    emojiUpdater.performDestroy();
  }

  @Override
  public final InputConnection onCreateInputConnection (EditorInfo editorInfo) {
    InputConnection ic = createInputConnection(editorInfo);
    if (ic != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !(ic instanceof EmojiInputConnection)) {
      return new EmojiInputConnection(this, ic);
    } else {
      return ic;
    }
  }

  protected InputConnection createInputConnection (EditorInfo editorInfo) {
    return super.onCreateInputConnection(editorInfo);
  }
}
