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
package io.relimus.flatgram.component.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;

import io.relimus.flatgram.R;
import io.relimus.flatgram.U;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.data.MediaWrapper;
import io.relimus.flatgram.data.TD;
import io.relimus.flatgram.loader.ImageFile;
import io.relimus.flatgram.loader.ImageFileLocal;
import io.relimus.flatgram.loader.ImageVideoThumbFile;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.tool.Screen;
import io.relimus.flatgram.tool.Strings;
import io.relimus.flatgram.tool.TGMimeType;

import me.vkryl.core.StringUtils;

public final class DownloadedFileItem {
  public final TdApi.FileDownload download;
  public final TdApi.Message message;
  public final TdApi.File file;
  public final int fileId;
  public final boolean completed;
  public final boolean paused;
  public final long downloadedSize;
  public final long totalSize;
  public final @NonNull String title;
  public final @NonNull String subtitle;
  public final @NonNull String source;
  public final @Nullable String localPath;
  public final @Nullable String mimeType;
  public final @Nullable ImageFile miniThumbnail;
  public final @Nullable ImageFile preview;

  public DownloadedFileItem (@NonNull Tdlib tdlib, @NonNull TdApi.FileDownload download,
                             boolean completed) {
    this.download = download;
    this.message = download.message;
    this.fileId = download.fileId;
    this.paused = download.isPaused;
    this.file = download.message != null && download.message.content != null ?
      TD.getFile(download.message) : null;
    this.completed = completed || download.completeDate != 0;
    this.downloadedSize = file != null && file.local != null ? file.local.downloadedSize : 0;
    this.totalSize = file != null ? file.size != 0 ? file.size : file.expectedSize : 0;
    this.localPath = file != null && file.local != null ? file.local.path : null;
    this.mimeType = getMimeType(download.message);
    Preview preview = getPreview(tdlib, download.message, null);
    this.miniThumbnail = preview.miniThumbnail;
    this.preview = preview.preview;
    this.title = getTitle(download.message, file);
    this.source = getSource(tdlib, download.message);
    this.subtitle = buildSubtitle(this.completed, downloadedSize, totalSize, source);
  }

  private DownloadedFileItem (@NonNull Tdlib tdlib, @NonNull DownloadedFileItem item,
                              @NonNull TdApi.FileDownload download, boolean completed,
                              boolean paused, @Nullable TdApi.File file) {
    this.download = download;
    this.message = item.message;
    this.file = file;
    this.fileId = item.fileId;
    this.completed = completed;
    this.paused = paused;
    this.downloadedSize = file != null && file.local != null ? file.local.downloadedSize : 0;
    this.totalSize = file != null ? file.size != 0 ? file.size : file.expectedSize : 0;
    this.localPath = file != null && file.local != null ? file.local.path : null;
    this.mimeType = item.mimeType;
    this.title = item.title;
    this.source = item.source;
    ImageFile miniThumbnail = item.miniThumbnail;
    ImageFile preview = item.preview;
    if (preview == null && file != null && TD.isFileLoaded(file)) {
      Preview updatedPreview = getPreview(tdlib, message, file);
      if (miniThumbnail == null) {
        miniThumbnail = updatedPreview.miniThumbnail;
      }
      preview = updatedPreview.preview;
    }
    this.miniThumbnail = miniThumbnail;
    this.preview = preview;
    this.subtitle = buildSubtitle(completed, downloadedSize, totalSize, source);
  }

  public boolean isActive () {
    return !completed;
  }

  public @NonNull DownloadedFileItem withFile (@NonNull Tdlib tdlib, @NonNull TdApi.File file) {
    return new DownloadedFileItem(tdlib, this, download, completed, paused, file);
  }

  public @NonNull DownloadedFileItem withDownloadState (@NonNull Tdlib tdlib, int completeDate,
                                                        boolean paused) {
    TdApi.FileDownload download = new TdApi.FileDownload(
      fileId, message, this.download.addDate, completeDate, paused
    );
    return new DownloadedFileItem(tdlib, this, download, completeDate != 0, paused, file);
  }

  public boolean isSameContent (@NonNull DownloadedFileItem item) {
    return completed == item.completed &&
      paused == item.paused &&
      downloadedSize == item.downloadedSize &&
      totalSize == item.totalSize &&
      StringUtils.equalsOrBothEmpty(title, item.title) &&
      StringUtils.equalsOrBothEmpty(subtitle, item.subtitle) &&
      StringUtils.equalsOrBothEmpty(source, item.source) &&
      sameImageFile(miniThumbnail, item.miniThumbnail) &&
      sameImageFile(preview, item.preview);
  }

  public static @NonNull String buildSubtitle (boolean completed, long downloadedSize,
                                               long totalSize, @Nullable String source) {
    String size = completed ?
      Strings.buildSize(Math.max(totalSize, downloadedSize)) :
      Strings.buildSize(downloadedSize) + " / " +
        Strings.buildSize(Math.max(totalSize, downloadedSize));
    return StringUtils.isEmpty(source) ? size : size + " · " + source;
  }

  private static @NonNull String getTitle (@Nullable TdApi.Message message,
                                           @Nullable TdApi.File file) {
    if (message != null && message.content != null) {
      switch (message.content.getConstructor()) {
        case TdApi.MessageDocument.CONSTRUCTOR: {
          String fileName = ((TdApi.MessageDocument) message.content).document.fileName;
          if (!StringUtils.isEmpty(fileName)) {
            return fileName;
          }
          break;
        }
        case TdApi.MessageAudio.CONSTRUCTOR: {
          TdApi.Audio audio = ((TdApi.MessageAudio) message.content).audio;
          if (!StringUtils.isEmpty(audio.fileName)) {
            return audio.fileName;
          }
          if (!StringUtils.isEmpty(audio.title)) {
            return audio.title;
          }
          break;
        }
        case TdApi.MessageVideo.CONSTRUCTOR: {
          String fileName = ((TdApi.MessageVideo) message.content).video.fileName;
          if (!StringUtils.isEmpty(fileName)) {
            return fileName;
          }
          break;
        }
        case TdApi.MessageAnimation.CONSTRUCTOR: {
          String fileName = ((TdApi.MessageAnimation) message.content).animation.fileName;
          if (!StringUtils.isEmpty(fileName)) {
            return fileName;
          }
          break;
        }
      }
    }
    if (file != null && file.local != null && !StringUtils.isEmpty(file.local.path)) {
      return U.getFileName(file.local.path);
    }
    return Lang.getString(R.string.File);
  }

  private static @Nullable String getMimeType (@Nullable TdApi.Message message) {
    if (message == null || message.content == null) {
      return null;
    }
    switch (message.content.getConstructor()) {
      case TdApi.MessageDocument.CONSTRUCTOR:
        return ((TdApi.MessageDocument) message.content).document.mimeType;
      case TdApi.MessageAudio.CONSTRUCTOR:
        return ((TdApi.MessageAudio) message.content).audio.mimeType;
      case TdApi.MessageVideo.CONSTRUCTOR:
        return ((TdApi.MessageVideo) message.content).video.mimeType;
      case TdApi.MessageAnimation.CONSTRUCTOR:
        return ((TdApi.MessageAnimation) message.content).animation.mimeType;
      case TdApi.MessageVoiceNote.CONSTRUCTOR:
        return ((TdApi.MessageVoiceNote) message.content).voiceNote.mimeType;
    }
    return null;
  }

  private static @NonNull Preview getPreview (@NonNull Tdlib tdlib, @Nullable TdApi.Message message,
                                               @Nullable TdApi.File updatedFile) {
    if (message == null || message.content == null) {
      return Preview.EMPTY;
    }
    switch (message.content.getConstructor()) {
      case TdApi.MessageVideo.CONSTRUCTOR: {
        TdApi.Video video = ((TdApi.MessageVideo) message.content).video;
        return new Preview(toMiniThumbnail(video.minithumbnail), toVideoPreview(tdlib,
          video.thumbnail, selectFile(video.video, updatedFile), true));
      }
      case TdApi.MessageAnimation.CONSTRUCTOR: {
        TdApi.Animation animation = ((TdApi.MessageAnimation) message.content).animation;
        return new Preview(toMiniThumbnail(animation.minithumbnail), toVideoPreview(tdlib,
          animation.thumbnail, selectFile(animation.animation, updatedFile),
          isVideoMimeType(animation.mimeType)));
      }
      case TdApi.MessageDocument.CONSTRUCTOR: {
        TdApi.Document document = ((TdApi.MessageDocument) message.content).document;
        boolean isVideo = isVideoMimeType(document.mimeType);
        return new Preview(toMiniThumbnail(document.minithumbnail), isVideo ? toVideoPreview(tdlib,
          document.thumbnail, selectFile(document.document, updatedFile), true) :
          toPreview(tdlib, document.thumbnail));
      }
      case TdApi.MessageAudio.CONSTRUCTOR: {
        TdApi.Audio audio = ((TdApi.MessageAudio) message.content).audio;
        return new Preview(toMiniThumbnail(audio.albumCoverMinithumbnail),
          toPreview(tdlib, audio.albumCoverThumbnail));
      }
      case TdApi.MessagePhoto.CONSTRUCTOR: {
        TdApi.Photo photo = ((TdApi.MessagePhoto) message.content).photo;
        TdApi.PhotoSize size = MediaWrapper.buildPreviewSize(photo);
        return new Preview(toMiniThumbnail(photo.minithumbnail), toPreview(tdlib, size));
      }
      case TdApi.MessageVideoNote.CONSTRUCTOR: {
        TdApi.VideoNote videoNote = ((TdApi.MessageVideoNote) message.content).videoNote;
        return new Preview(toMiniThumbnail(videoNote.minithumbnail), toVideoPreview(tdlib,
          videoNote.thumbnail, selectFile(videoNote.video, updatedFile), true));
      }
    }
    return Preview.EMPTY;
  }

  private static @Nullable TdApi.File selectFile (@Nullable TdApi.File file,
                                                   @Nullable TdApi.File updatedFile) {
    return file != null && updatedFile != null && file.id == updatedFile.id ? updatedFile : file;
  }

  private static @Nullable ImageFile toMiniThumbnail (@Nullable TdApi.Minithumbnail minithumbnail) {
    if (minithumbnail == null) {
      return null;
    }
    ImageFileLocal file = new ImageFileLocal(minithumbnail);
    file.setScaleType(ImageFile.CENTER_CROP);
    file.setDecodeSquare(true);
    file.setSize(Screen.dp(48f, 3f));
    return file;
  }

  private static @Nullable ImageFile toPreview (@NonNull Tdlib tdlib,
                                                @Nullable TdApi.Thumbnail thumbnail) {
    ImageFile file = TD.toImageFile(tdlib, thumbnail);
    if (file != null) {
      setupPreview(file);
    }
    return file;
  }

  private static @Nullable ImageFile toPreview (@NonNull Tdlib tdlib,
                                                @Nullable TdApi.PhotoSize size) {
    if (size == null) {
      return null;
    }
    ImageFile file = new ImageFile(tdlib, size.photo);
    setupPreview(file);
    return file;
  }

  private static @Nullable ImageFile toVideoPreview (@NonNull Tdlib tdlib,
                                                     @Nullable TdApi.Thumbnail thumbnail,
                                                     @Nullable TdApi.File file,
                                                     boolean isVideo) {
    ImageFile preview = toPreview(tdlib, thumbnail);
    if (preview == null && isVideo && TD.isFileLoaded(file)) {
      ImageVideoThumbFile videoPreview = new ImageVideoThumbFile(tdlib, file);
      videoPreview.setFrameTimeUs(0);
      preview = videoPreview;
      setupPreview(preview);
    }
    return preview;
  }

  private static void setupPreview (@NonNull ImageFile file) {
    file.setDecodeSquare(true);
    file.setScaleType(ImageFile.CENTER_CROP);
    file.setSize(Screen.dp(48f, 3f));
    file.setNoBlur();
  }

  private static boolean sameImageFile (@Nullable ImageFile a, @Nullable ImageFile b) {
    return a == b || a != null && b != null && a.getId() == b.getId();
  }

  private static boolean isVideoMimeType (@Nullable String mimeType) {
    return !StringUtils.isEmpty(mimeType) &&
      (TGMimeType.isVideoMimeType(mimeType) || mimeType.startsWith("video/"));
  }

  private static @NonNull String getSource (@NonNull Tdlib tdlib, @Nullable TdApi.Message message) {
    if (message != null) {
      String title = tdlib.chatTitle(message.chatId);
      if (!StringUtils.isEmpty(title)) {
        return title;
      }
    }
    return "";
  }

  private static final class Preview {
    private static final Preview EMPTY = new Preview(null, null);

    private final @Nullable ImageFile miniThumbnail;
    private final @Nullable ImageFile preview;

    private Preview (@Nullable ImageFile miniThumbnail, @Nullable ImageFile preview) {
      this.miniThumbnail = miniThumbnail;
      this.preview = preview;
    }
  }
}
