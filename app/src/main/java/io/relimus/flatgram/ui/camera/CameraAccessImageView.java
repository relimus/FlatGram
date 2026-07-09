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
 * File created on 21/09/2017
 */
package io.relimus.flatgram.ui.camera;

import android.content.Context;
import android.view.MotionEvent;

import io.relimus.flatgram.U;
import io.relimus.flatgram.component.chat.InvisibleImageView;
import io.relimus.flatgram.navigation.ViewController;
import io.relimus.flatgram.tool.UI;
import io.relimus.flatgram.ui.MessagesController;
import io.relimus.flatgram.unsorted.Settings;

public class CameraAccessImageView extends InvisibleImageView {
  private final MessagesController c;
  private boolean hasAnyCamera;

  private ViewController.CameraOpenOptions cameraOpenOptions;

  public CameraAccessImageView (Context context, MessagesController c) {
    super(context);
    this.c = c;
    this.hasAnyCamera = U.deviceHasAnyCamera(context);
  }
    @Override
    public boolean onTouchEvent (MotionEvent event) {
    boolean res = super.onTouchEvent(event);
    if (Settings.instance().getCameraType() != Settings.CAMERA_TYPE_SYSTEM && res && event.getAction() == MotionEvent.ACTION_DOWN) {
      if (!hasAnyCamera) {
        hasAnyCamera = U.deviceHasAnyCamera(getContext());
      }
      if (hasAnyCamera && c.canSendPhotosAndVideos()) {
        if (cameraOpenOptions == null)
          cameraOpenOptions = new ViewController.CameraOpenOptions();
        UI.getContext(getContext()).prepareCameraDragByTouchDown(cameraOpenOptions, true);
      }
    }
    return res;
  }

  public void setCameraOpenOptions (ViewController.CameraOpenOptions options) {
    this.cameraOpenOptions = options;
  }
}
