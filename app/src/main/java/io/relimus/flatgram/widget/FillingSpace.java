package io.relimus.flatgram.widget;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import io.relimus.flatgram.navigation.ViewController;
import io.relimus.flatgram.support.ViewSupport;
import io.relimus.flatgram.theme.ColorId;
import io.relimus.flatgram.tool.Views;

import me.vkryl.core.lambda.Destroyable;

public class FillingSpace extends View implements Destroyable {
  public FillingSpace (Context context) {
    super(context);
    setVisibility(View.GONE);
  }

  public boolean setLayoutHeight (int height, boolean updateVisibility) {
    boolean updated = Views.setLayoutHeight(this, height);
    if (updateVisibility) {
      setVisibility(height > 0 ? View.VISIBLE : View.GONE);
    }
    return updated;
  }

  private ViewController<?> themeProvider;

  public void setThemedBackground (@ColorId int colorId, @Nullable ViewController<?> themeProvider) {
    this.themeProvider = themeProvider;
    ViewSupport.setThemedBackground(this, colorId, themeProvider);
  }

  @Override
  public void performDestroy () {
    if (themeProvider != null) {
      themeProvider.removeThemeListenerByTarget(this);
      themeProvider = null;
    }
  }
}
