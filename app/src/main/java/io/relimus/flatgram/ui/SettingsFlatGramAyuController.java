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
package io.relimus.flatgram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import io.relimus.flatgram.R;
import io.relimus.flatgram.component.base.SettingView;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.tool.Screen;
import io.relimus.flatgram.unsorted.Settings;
import io.relimus.flatgram.v.CustomRecyclerView;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.core.lambda.Destroyable;

public class SettingsFlatGramAyuController extends RecyclerViewController<Void> implements
  View.OnClickListener {
  private static final int VIEW_TYPE_DELETED_MESSAGES_PREVIEW = 0;

  private SettingsAdapter adapter;
  private DeletedMessagesPreviewView deletedMessagesPreviewView;

  public SettingsFlatGramAyuController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_flatGramAyuSettings;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.FlatGramAyuSettings);
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this) {
      @Override
      public void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
        if (item.getId() == R.id.btn_flatGramSaveDeletedMessages) {
          view.getToggler().setRadioEnabled(Settings.instance().saveDeletedMessages(), isUpdate);
        } else if (item.getId() == R.id.btn_flatGramTranslucentDeletedMessages) {
          view.getToggler().setRadioEnabled(Settings.instance().flatGramTranslucentDeletedMessages(), isUpdate);
        } else if (item.getId() == R.id.btn_flatGramDeletedMarkStyle) {
          view.setData(getDeletedMarkName(Settings.instance().getFlatGramDeletedMarkStyle()));
        }
      }

      @Override
      protected SettingHolder initCustom (ViewGroup parent, int customViewType) {
        if (customViewType == VIEW_TYPE_DELETED_MESSAGES_PREVIEW) {
          deletedMessagesPreviewView = new DeletedMessagesPreviewView(parent.getContext(), tdlib);
          SettingsFlatGramAyuController.this.addDestroyListener(deletedMessagesPreviewView);
          return new SettingHolder(deletedMessagesPreviewView);
        }
        throw new IllegalArgumentException("customViewType=" + customViewType);
      }
    };
    adapter.setItems(new ListItem[] {
      new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL),
      new ListItem(ListItem.TYPE_SHADOW_TOP),
      new ListItem(ListItem.TYPE_CUSTOM - VIEW_TYPE_DELETED_MESSAGES_PREVIEW,
        R.id.btn_flatGramDeletedMessagesPreview),
      new ListItem(ListItem.TYPE_SEPARATOR_FULL),
      new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_flatGramSaveDeletedMessages,
        0, R.string.FlatGramSaveDeletedMessages),
      new ListItem(ListItem.TYPE_SEPARATOR_FULL),
      new ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_flatGramTranslucentDeletedMessages,
        0, R.string.FlatGramTranslucentDeletedMessages),
      new ListItem(ListItem.TYPE_SEPARATOR_FULL),
      new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_flatGramDeletedMarkStyle,
        0, R.string.FlatGramDeletedMarkStyle),
      new ListItem(ListItem.TYPE_SHADOW_BOTTOM)
    }, false);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onClick (View v) {
    if (v.getId() == R.id.btn_flatGramSaveDeletedMessages) {
      Settings.instance().setSaveDeletedMessages(adapter.toggleView(v));
    } else if (v.getId() == R.id.btn_flatGramTranslucentDeletedMessages) {
      Settings.instance().setFlatGramTranslucentDeletedMessages(adapter.toggleView(v));
      reloadDeletedMessagesPreview();
    } else if (v.getId() == R.id.btn_flatGramDeletedMarkStyle) {
      showDeletedMarkOptions();
    }
  }

  private void showDeletedMarkOptions () {
    int style = Settings.instance().getFlatGramDeletedMarkStyle();
    showSettings(R.id.btn_flatGramDeletedMarkStyle, new ListItem[] {
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_flatGramDeletedMarkNothing, 0,
        R.string.FlatGramDeletedMarkNothing, R.id.btn_flatGramDeletedMarkStyle,
        style == Settings.FLATGRAM_DELETED_MARK_NOTHING),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_flatGramDeletedMarkTrash, 0,
        R.string.FlatGramDeletedMarkTrash, R.id.btn_flatGramDeletedMarkStyle,
        style == Settings.FLATGRAM_DELETED_MARK_TRASH),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_flatGramDeletedMarkCross, 0,
        R.string.FlatGramDeletedMarkCross, R.id.btn_flatGramDeletedMarkStyle,
        style == Settings.FLATGRAM_DELETED_MARK_CROSS),
      new ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_flatGramDeletedMarkEyeCrossed, 0,
        R.string.FlatGramDeletedMarkEyeCrossed, R.id.btn_flatGramDeletedMarkStyle,
        style == Settings.FLATGRAM_DELETED_MARK_EYE_CROSSED)
    }, (id, result) -> {
      Settings.instance().setFlatGramDeletedMarkStyle(
        getDeletedMarkStyle(result.get(R.id.btn_flatGramDeletedMarkStyle))
      );
      adapter.updateValuedSettingById(R.id.btn_flatGramDeletedMarkStyle);
      reloadDeletedMessagesPreview();
    });
  }

  private int getDeletedMarkStyle (int id) {
    if (id == R.id.btn_flatGramDeletedMarkNothing) {
      return Settings.FLATGRAM_DELETED_MARK_NOTHING;
    } else if (id == R.id.btn_flatGramDeletedMarkCross) {
      return Settings.FLATGRAM_DELETED_MARK_CROSS;
    } else if (id == R.id.btn_flatGramDeletedMarkEyeCrossed) {
      return Settings.FLATGRAM_DELETED_MARK_EYE_CROSSED;
    }
    return Settings.FLATGRAM_DELETED_MARK_TRASH;
  }

  private String getDeletedMarkName (int style) {
    switch (style) {
      case Settings.FLATGRAM_DELETED_MARK_NOTHING:
        return Lang.getString(R.string.FlatGramDeletedMarkNothing);
      case Settings.FLATGRAM_DELETED_MARK_CROSS:
        return Lang.getString(R.string.FlatGramDeletedMarkCross);
      case Settings.FLATGRAM_DELETED_MARK_EYE_CROSSED:
        return Lang.getString(R.string.FlatGramDeletedMarkEyeCrossed);
      case Settings.FLATGRAM_DELETED_MARK_TRASH:
      default:
        return Lang.getString(R.string.FlatGramDeletedMarkTrash);
    }
  }

  private void reloadDeletedMessagesPreview () {
    if (deletedMessagesPreviewView != null) {
      deletedMessagesPreviewView.reload();
    }
  }

  private static class DeletedMessagesPreviewView extends FrameLayout implements Destroyable {
    private final Tdlib tdlib;
    private MessagesController controller;
    private View controllerView;
    private ValueAnimator heightAnimator;
    private boolean destroyed;

    public DeletedMessagesPreviewView (Context context, Tdlib tdlib) {
      super(context);
      this.tdlib = tdlib;
      setClipChildren(false);
      setClipToPadding(false);
      setLayoutParams(new RecyclerView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        Screen.dp(96f)
      ));
      reload();
    }

    public void reload () {
      if (destroyed) return;
      if (controller != null) {
        controller.reloadAyuDeletedPreview();
        return;
      }
      clearController();
      controller = new MessagesController(getContext(), tdlib);
      controller.setArguments(new MessagesController.Arguments(
        MessagesController.PREVIEW_MODE_AYU_DELETED, null, null
      ));
      controller.setAyuDeletedPreviewLayoutListener(this::resizeToPreviewContent);
      controllerView = controller.getValue();
      controllerView.setLayoutParams(new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      ));
      addView(controllerView);
    }

    private void resizeToPreviewContent () {
      if (controller == null) return;
      int height = controller.getAyuDeletedPreviewContentHeight();
      if (height <= 0) return;
      ViewGroup.LayoutParams params = getLayoutParams();
      if (params == null || params.height == height) return;

      if (heightAnimator != null) {
        heightAnimator.cancel();
      }
      int currentHeight = getMeasuredHeight() != 0 ? getMeasuredHeight() : params.height;
      heightAnimator = ValueAnimator.ofInt(currentHeight, height);
      heightAnimator.setDuration(160L);
      heightAnimator.setInterpolator(AnimatorUtils.DECELERATE_INTERPOLATOR);
      heightAnimator.addUpdateListener(animation -> {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
          layoutParams.height = (int) animation.getAnimatedValue();
          setLayoutParams(layoutParams);
        }
      });
      heightAnimator.start();
    }

    @Override
    public void performDestroy () {
      destroyed = true;
      clearController();
    }

    private void clearController () {
      if (heightAnimator != null) {
        heightAnimator.cancel();
        heightAnimator = null;
      }
      if (controllerView != null) {
        removeView(controllerView);
        controllerView = null;
      }
      if (controller != null) {
        controller.setAyuDeletedPreviewLayoutListener(null);
        controller.performDestroy();
        controller = null;
      }
    }
  }
}
