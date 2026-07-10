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
import androidx.annotation.UiThread;
import androidx.collection.SparseArrayCompat;

import org.drinkless.tdlib.TdApi;

import java.util.ArrayList;
import java.util.List;

import io.relimus.flatgram.telegram.DownloadsListUpdateListener;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.telegram.TdlibFilesManager;

public final class DownloadsSearchManager implements DownloadsListUpdateListener,
                                                     TdlibFilesManager.FileListener {
  public interface Listener {
    void onDownloadsLoading ();
    void onDownloadsChanged (@NonNull List<DownloadedFileItem> active,
                             @NonNull List<DownloadedFileItem> completed);
    void onDownloadUpdated (@NonNull DownloadedFileItem item);
    void onDownloadsError ();
  }

  private static final int LIMIT = 100;
  private static final long SEARCH_DELAY_MS = 300L;
  private static final long PROGRESS_UPDATE_INTERVAL_MS = 500L;

  private final Tdlib tdlib;
  private final Listener listener;
  private final ArrayList<DownloadedFileItem> activeDownloads = new ArrayList<>();
  private final ArrayList<DownloadedFileItem> completedDownloads = new ArrayList<>();
  private final SparseArrayCompat<DownloadedFileItem> downloadsByFileId =
    new SparseArrayCompat<>();
  private final Object progressLock = new Object();
  private final SparseArrayCompat<TdApi.File> pendingProgressFiles =
    new SparseArrayCompat<>();
  private final Runnable progressUpdateRunnable = this::dispatchPendingProgress;
  private @Nullable Runnable searchRunnable;
  private int contextId;
  private String query = "";
  private boolean isActive;
  private boolean isDestroyed;
  private boolean isSubscribed;
  private boolean filesSubscribed;
  private boolean hasLoaded;
  private boolean requestInProgress;
  private boolean loadingNotified;
  private boolean refreshAfterRequest;
  private boolean refreshScheduled;
  private boolean progressUpdateScheduled;
  private final Runnable refreshRunnable = () -> {
    refreshScheduled = false;
    if (isActive && !isDestroyed) {
      scheduleSearch(query, false, 0L);
    }
  };

  public DownloadsSearchManager (@NonNull Tdlib tdlib, @NonNull Listener listener) {
    this.tdlib = tdlib;
    this.listener = listener;
  }

  @UiThread
  public void search (@NonNull String query) {
    if (isDestroyed) {
      return;
    }
    boolean wasActive = isActive;
    isActive = true;
    subscribeToUpdates();
    subscribeToFiles();
    if (this.query.equals(query)) {
      if (!wasActive || !hasLoaded && !requestInProgress && searchRunnable == null) {
        scheduleSearch(query, !hasLoaded, 0L);
      }
      return;
    }
    this.query = query;
    long delay = !query.isEmpty() &&
      (hasLoaded || requestInProgress || searchRunnable != null) ? SEARCH_DELAY_MS : 0L;
    scheduleSearch(query, !hasLoaded, delay);
  }

  @UiThread
  public void refresh () {
    if (!isDestroyed) {
      scheduleSearch(query, !hasLoaded, 0L);
    }
  }

  @UiThread
  public void togglePaused (@NonNull DownloadedFileItem item) {
    DownloadedFileItem currentItem = downloadsByFileId.get(item.fileId);
    if (currentItem == null) {
      currentItem = item;
    }
    if (requestInProgress) {
      refreshAfterRequest = true;
    }
    boolean isPaused = !currentItem.paused;
    applyDownloadState(currentItem.fileId, currentItem.download.completeDate, isPaused);
    tdlib.client().send(new TdApi.ToggleDownloadIsPaused(currentItem.fileId, isPaused),
      object -> {
        if (object.getConstructor() != TdApi.Ok.CONSTRUCTOR) {
          refreshIfActive();
        }
      });
  }

  @UiThread
  public void remove (@NonNull List<DownloadedFileItem> items) {
    if (requestInProgress) {
      refreshAfterRequest = true;
    }
    boolean changed = false;
    boolean needsBackfill = false;
    for (DownloadedFileItem item : items) {
      DownloadedFileItem currentItem = downloadsByFileId.get(item.fileId);
      if (currentItem != null &&
          (currentItem.completed ? completedDownloads : activeDownloads).size() >= LIMIT) {
        needsBackfill = true;
      }
      changed |= removeLocal(item.fileId);
    }
    if (changed) {
      listener.onDownloadsChanged(activeDownloads, completedDownloads);
    }
    if (items.isEmpty()) {
      return;
    }
    final boolean refreshAfterRemove = needsBackfill;
    final int[] remaining = {items.size()};
    final boolean[] failed = {false};
    for (DownloadedFileItem item : items) {
      tdlib.client().send(new TdApi.RemoveFileFromDownloads(item.fileId, true),
        object -> tdlib.ui().post(() -> {
          failed[0] |= object.getConstructor() != TdApi.Ok.CONSTRUCTOR;
          if (--remaining[0] == 0 && (refreshAfterRemove || failed[0])) {
            refreshIfActive();
          }
        }));
    }
  }

  @UiThread
  public void cancel () {
    isActive = false;
    contextId++;
    requestInProgress = false;
    refreshAfterRequest = false;
    loadingNotified = false;
    cancelSearch();
    cancelProgressUpdates();
    refreshScheduled = false;
    tdlib.ui().removeCallbacks(refreshRunnable);
    unsubscribeFromFiles();
    unsubscribeFromUpdates();
  }

  @UiThread
  public void destroy () {
    cancel();
    isDestroyed = true;
    activeDownloads.clear();
    completedDownloads.clear();
    downloadsByFileId.clear();
  }

  @Override
  public void updateFileAddedToDownloads (TdApi.FileDownload fileDownload,
                                          TdApi.DownloadedFileCounts counts) {
    tdlib.ui().post(() -> {
      if (!isActive || isDestroyed || deferListUpdate()) {
        return;
      }
      if (!query.isEmpty()) {
        scheduleSearch(query, false, SEARCH_DELAY_MS);
        return;
      }
      DownloadedFileItem item = new DownloadedFileItem(
        tdlib, fileDownload, fileDownload.completeDate != 0
      );
      removeLocal(item.fileId);
      addLocal(item);
      listener.onDownloadsChanged(activeDownloads, completedDownloads);
    });
  }

  @Override
  public void updateFileDownload (int fileId, int completeDate, boolean isPaused,
                                  TdApi.DownloadedFileCounts counts) {
    tdlib.ui().post(() -> {
      if (!isActive || isDestroyed || deferListUpdate()) {
        return;
      }
      applyDownloadState(fileId, completeDate, isPaused);
    });
  }

  @Override
  public void updateFileRemovedFromDownloads (int fileId, TdApi.DownloadedFileCounts counts) {
    tdlib.ui().post(() -> {
      if (!isActive || isDestroyed || deferListUpdate()) {
        return;
      }
      DownloadedFileItem item = downloadsByFileId.get(fileId);
      boolean needsBackfill = item != null &&
        (item.completed ? completedDownloads : activeDownloads).size() >= LIMIT;
      if (removeLocal(fileId)) {
        listener.onDownloadsChanged(activeDownloads, completedDownloads);
        if (needsBackfill) {
          refreshIfActive();
        }
      }
    });
  }

  @Override
  public void onFileLoadProgress (TdApi.File file) {
    scheduleFileUpdate(file, false);
  }

  @Override
  public void onFileLoadStateChanged (Tdlib tdlib, int fileId, int state,
                                      @Nullable TdApi.File downloadedFile) {
    if (downloadedFile != null) {
      scheduleFileUpdate(
        downloadedFile, state == TdlibFilesManager.STATE_DOWNLOADED_OR_UPLOADED
      );
    }
  }

  private void scheduleSearch (@NonNull String query, boolean showLoading, long delay) {
    cancelSearch();
    requestInProgress = false;
    refreshAfterRequest = false;
    final int currentContextId = ++contextId;
    Runnable runnable = () -> {
      searchRunnable = null;
      if (!isCurrentContext(currentContextId)) {
        return;
      }
      requestInProgress = true;
      if (showLoading && !loadingNotified) {
        loadingNotified = true;
        listener.onDownloadsLoading();
      }
      tdlib.client().send(
        new TdApi.SearchFileDownloads(query, true, false, "", LIMIT),
        object -> {
          ArrayList<DownloadedFileItem> active =
            object.getConstructor() == TdApi.FoundFileDownloads.CONSTRUCTOR ?
              buildItems(((TdApi.FoundFileDownloads) object).files, false) : null;
          tdlib.ui().post(() -> onActiveDownloadsFound(
            currentContextId, query, object, active
          ));
        }
      );
    };
    if (delay > 0L) {
      searchRunnable = runnable;
      tdlib.ui().postDelayed(runnable, delay);
    } else {
      runnable.run();
    }
  }

  private void onActiveDownloadsFound (int currentContextId, @NonNull String query,
                                       @NonNull TdApi.Object object,
                                       @Nullable ArrayList<DownloadedFileItem> active) {
    if (!isCurrentContext(currentContextId)) {
      return;
    }
    if (object.getConstructor() != TdApi.FoundFileDownloads.CONSTRUCTOR || active == null) {
      onSearchError();
      return;
    }
    tdlib.client().send(
      new TdApi.SearchFileDownloads(query, false, true, "", LIMIT),
      result -> {
        ArrayList<DownloadedFileItem> completed =
          result.getConstructor() == TdApi.FoundFileDownloads.CONSTRUCTOR ?
            buildItems(((TdApi.FoundFileDownloads) result).files, true) : null;
        tdlib.ui().post(() -> onCompletedDownloadsFound(
          currentContextId, active, result, completed
        ));
      }
    );
  }

  private void onCompletedDownloadsFound (int currentContextId,
                                          @Nullable ArrayList<DownloadedFileItem> active,
                                          @NonNull TdApi.Object object,
                                          @Nullable ArrayList<DownloadedFileItem> completed) {
    if (!isCurrentContext(currentContextId)) {
      return;
    }
    if (object.getConstructor() != TdApi.FoundFileDownloads.CONSTRUCTOR ||
        active == null || completed == null) {
      onSearchError();
      return;
    }
    if (refreshAfterRequest) {
      scheduleSearch(query, false, 0L);
      return;
    }
    requestInProgress = false;
    loadingNotified = false;
    hasLoaded = true;
    setDownloads(active, completed);
    listener.onDownloadsChanged(activeDownloads, completedDownloads);
  }

  private @NonNull ArrayList<DownloadedFileItem> buildItems (
    @Nullable TdApi.FileDownload[] files, boolean completed
  ) {
    ArrayList<DownloadedFileItem> items = new ArrayList<>(files != null ? files.length : 0);
    if (files != null) {
      for (TdApi.FileDownload file : files) {
        if (file != null) {
          items.add(new DownloadedFileItem(tdlib, file, completed));
        }
      }
    }
    return items;
  }

  private void onSearchError () {
    requestInProgress = false;
    loadingNotified = false;
    listener.onDownloadsError();
  }

  private boolean isCurrentContext (int currentContextId) {
    return currentContextId == contextId && isActive && !isDestroyed;
  }

  private boolean deferListUpdate () {
    if (requestInProgress) {
      refreshAfterRequest = true;
      return true;
    }
    return searchRunnable != null;
  }

  private void cancelSearch () {
    if (searchRunnable != null) {
      tdlib.ui().removeCallbacks(searchRunnable);
      searchRunnable = null;
    }
  }

  private void refreshIfActive () {
    tdlib.ui().post(() -> {
      if (!refreshScheduled && isActive && !isDestroyed) {
        refreshScheduled = true;
        tdlib.ui().post(refreshRunnable);
      }
    });
  }

  private void subscribeToUpdates () {
    if (!isSubscribed) {
      isSubscribed = true;
      tdlib.listeners().subscribeToDownloadsListUpdates(this);
    }
  }

  private void unsubscribeFromUpdates () {
    if (isSubscribed) {
      isSubscribed = false;
      tdlib.listeners().unsubscribeFromDownloadsListUpdates(this);
    }
  }

  private void subscribeToFiles () {
    if (!filesSubscribed) {
      filesSubscribed = true;
      for (DownloadedFileItem item : activeDownloads) {
        if (item.file != null) {
          tdlib.files().subscribe(item.file, this);
        }
      }
    }
  }

  private void unsubscribeFromFiles () {
    if (filesSubscribed) {
      filesSubscribed = false;
      for (DownloadedFileItem item : activeDownloads) {
        tdlib.files().unsubscribe(item.fileId, this);
      }
    }
  }

  private void setDownloads (@NonNull List<DownloadedFileItem> active,
                             @NonNull List<DownloadedFileItem> completed) {
    unsubscribeFromFiles();
    activeDownloads.clear();
    activeDownloads.addAll(active);
    completedDownloads.clear();
    completedDownloads.addAll(completed);
    downloadsByFileId.clear();
    for (DownloadedFileItem item : activeDownloads) {
      downloadsByFileId.put(item.fileId, item);
    }
    for (DownloadedFileItem item : completedDownloads) {
      downloadsByFileId.put(item.fileId, item);
    }
    if (isActive) {
      subscribeToFiles();
    }
  }

  private void addLocal (@NonNull DownloadedFileItem item) {
    ArrayList<DownloadedFileItem> items = item.completed ?
      completedDownloads : activeDownloads;
    items.add(findInsertIndex(items, item), item);
    downloadsByFileId.put(item.fileId, item);
    if (!item.completed && filesSubscribed && item.file != null) {
      tdlib.files().subscribe(item.file, this);
    }
    if (items.size() > LIMIT) {
      DownloadedFileItem removedItem = items.remove(items.size() - 1);
      if (downloadsByFileId.get(removedItem.fileId) == removedItem) {
        downloadsByFileId.remove(removedItem.fileId);
      }
      if (!removedItem.completed && filesSubscribed) {
        tdlib.files().unsubscribe(removedItem.fileId, this);
      }
    }
  }

  private boolean removeLocal (int fileId) {
    DownloadedFileItem item = downloadsByFileId.get(fileId);
    if (item == null) {
      return false;
    }
    downloadsByFileId.remove(fileId);
    ArrayList<DownloadedFileItem> items = item.completed ?
      completedDownloads : activeDownloads;
    int index = indexOf(items, fileId);
    if (index != -1) {
      items.remove(index);
    }
    if (!item.completed && filesSubscribed) {
      tdlib.files().unsubscribe(fileId, this);
    }
    return true;
  }

  private void applyDownloadState (int fileId, int completeDate, boolean isPaused) {
    DownloadedFileItem item = downloadsByFileId.get(fileId);
    if (item == null) {
      return;
    }
    boolean completed = completeDate != 0;
    if (item.completed == completed && item.paused == isPaused &&
        item.download.completeDate == completeDate) {
      return;
    }
    DownloadedFileItem updatedItem = item.withDownloadState(tdlib, completeDate, isPaused);
    if (item.completed != updatedItem.completed ||
        item.download.completeDate != updatedItem.download.completeDate) {
      boolean needsBackfill = (item.completed ? completedDownloads : activeDownloads)
        .size() >= LIMIT;
      removeLocal(fileId);
      addLocal(updatedItem);
      listener.onDownloadsChanged(activeDownloads, completedDownloads);
      if (needsBackfill) {
        refreshIfActive();
      }
      return;
    }
    replaceLocal(item, updatedItem);
    listener.onDownloadUpdated(updatedItem);
  }

  private void replaceLocal (@NonNull DownloadedFileItem oldItem,
                             @NonNull DownloadedFileItem newItem) {
    ArrayList<DownloadedFileItem> items = oldItem.completed ?
      completedDownloads : activeDownloads;
    int index = indexOf(items, oldItem.fileId);
    if (index != -1) {
      items.set(index, newItem);
    }
    downloadsByFileId.put(newItem.fileId, newItem);
  }

  private static int findInsertIndex (@NonNull List<DownloadedFileItem> items,
                                      @NonNull DownloadedFileItem item) {
    int date = sortDate(item);
    for (int i = 0; i < items.size(); i++) {
      if (date >= sortDate(items.get(i))) {
        return i;
      }
    }
    return items.size();
  }

  private static int sortDate (@NonNull DownloadedFileItem item) {
    return item.download.addDate;
  }

  private static int indexOf (@NonNull List<DownloadedFileItem> items, int fileId) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).fileId == fileId) {
        return i;
      }
    }
    return -1;
  }

  private void scheduleFileUpdate (@NonNull TdApi.File file, boolean immediately) {
    synchronized (progressLock) {
      pendingProgressFiles.put(file.id, file);
      if (progressUpdateScheduled && !immediately) {
        return;
      }
      progressUpdateScheduled = true;
    }
    if (immediately) {
      tdlib.ui().post(progressUpdateRunnable);
    } else {
      tdlib.ui().postDelayed(progressUpdateRunnable, PROGRESS_UPDATE_INTERVAL_MS);
    }
  }

  private void dispatchPendingProgress () {
    ArrayList<TdApi.File> files;
    synchronized (progressLock) {
      progressUpdateScheduled = false;
      if (pendingProgressFiles.size() == 0) {
        return;
      }
      files = new ArrayList<>(pendingProgressFiles.size());
      for (int i = 0; i < pendingProgressFiles.size(); i++) {
        files.add(pendingProgressFiles.valueAt(i));
      }
      pendingProgressFiles.clear();
    }
    tdlib.ui().removeCallbacks(progressUpdateRunnable);
    if (!isActive || isDestroyed) {
      return;
    }
    for (TdApi.File file : files) {
      DownloadedFileItem item = downloadsByFileId.get(file.id);
      if (item != null) {
        DownloadedFileItem updatedItem = item.withFile(tdlib, file);
        replaceLocal(item, updatedItem);
        listener.onDownloadUpdated(updatedItem);
      }
    }
  }

  private void cancelProgressUpdates () {
    synchronized (progressLock) {
      progressUpdateScheduled = false;
      pendingProgressFiles.clear();
    }
    tdlib.ui().removeCallbacks(progressUpdateRunnable);
  }
}
