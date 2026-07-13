package io.relimus.flatgram.ayu;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import io.relimus.flatgram.Log;
import io.relimus.flatgram.core.BaseThread;
import io.relimus.flatgram.data.TD;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.telegram.TdlibManager;
import io.relimus.flatgram.unsorted.Settings;
import me.vkryl.core.FileUtils;
import me.vkryl.core.lambda.RunnableData;
import tgx.td.Td;

/**
 * Account-local deleted message storage, based on the Ayu behavior used by NagramXF.
 */
public final class AyuMessageStore {
  private static final long USER_DELETE_TIMEOUT = 30_000L;

  private final Tdlib tdlib;
  private final File accountDirectory;
  private final File mediaDirectory;
  private final BaseThread thread;
  private final BaseThread mediaThread;
  private final AyuDeletionTracker deletionTracker =
    new AyuDeletionTracker (USER_DELETE_TIMEOUT);
  private final Set<AyuMessageKey> deletedKeys = Collections.newSetFromMap (
    new ConcurrentHashMap<> ()
  );

  private volatile boolean available = true;
  private volatile boolean closed;
  private AyuMessageDatabase database;

  public AyuMessageStore (Tdlib tdlib) {
    this.tdlib = tdlib;
    accountDirectory = new File (TdlibManager.getTdlibDirectory (tdlib.id (), false));
    mediaDirectory = new File (accountDirectory, "ayu_media");
    thread = new BaseThread ("AyuStore-" + tdlib.id ());
    mediaThread = new BaseThread ("AyuMedia-" + tdlib.id ());
  }

  public void captureMessage (TdApi.Message message) {
    if (message == null || !Settings.instance ().saveDeletedMessages ()) return;
    post (() -> {
      if (!Settings.instance ().saveDeletedMessages ()) return;
      try {
        database.upsertLive (
          message,
          TdApiObjectCodec.encode (message),
          System.currentTimeMillis ()
        );
      } catch (Throwable t) {
        disable ("Unable to capture Ayu message", t);
      }
    });
  }

  public void updateMessageContent (
    long chatId,
    long messageId,
    TdApi.MessageContent content
  ) {
    if (!Settings.instance ().saveDeletedMessages ()) return;
    post (() -> {
      try {
        database.updateContent (
          chatId, messageId, content, System.currentTimeMillis ()
        );
      } catch (Throwable t) {
        disable ("Unable to update Ayu message content", t);
      }
    });
  }

  public void updateMessageEdited (
    long chatId,
    long messageId,
    int editDate,
    @Nullable TdApi.ReplyMarkup replyMarkup
  ) {
    if (!Settings.instance ().saveDeletedMessages ()) return;
    post (() -> {
      try {
        database.updateEdited (
          chatId, messageId, editDate, replyMarkup, System.currentTimeMillis ()
        );
      } catch (Throwable t) {
        disable ("Unable to update Ayu edited message", t);
      }
    });
  }

  public void processHistory (
    long chatId,
    TdApi.Message[] serverMessages,
    int direction,
    long fromMessageId,
    int limit,
    @Nullable TdApi.MessageTopic topicId,
    RunnableData<HistoryResult> callback
  ) {
    TdApi.Message[] messages = serverMessages != null ? serverMessages : new TdApi.Message[0];
    if (!Settings.instance ().saveDeletedMessages ()) {
      dispatch (callback, HistoryResult.serverOnly (messages));
      return;
    }
    post (() -> {
      HistoryResult result = HistoryResult.serverOnly (messages);
      if (Settings.instance ().saveDeletedMessages ()) {
        try {
          captureHistory (messages);
          AyuHistoryMerger.Window window = AyuHistoryMerger.resolveWindow (
            direction, fromMessageId, limit, messages
          );
          List<TdApi.Message> deletedMessages = new ArrayList<> ();
          for (byte[] snapshot : database.queryDeleted (
            chatId, window
          )) {
            try {
              TdApi.Message message = TdApiObjectCodec.decode (
                snapshot, TdApi.Message.class
              );
              if (topicId == null || Td.equalsTo (message.topicId, topicId)) {
                deletedMessages.add (message);
              }
            } catch (IOException e) {
              Log.w ("Unable to decode an Ayu message", e);
            }
          }
          result = new HistoryResult (
            AyuHistoryMerger.merge (messages, deletedMessages),
            messages.length,
            findNewestMessageId (messages),
            findOldestMessageId (messages)
          );
        } catch (Throwable t) {
          disable ("Unable to merge Ayu message history", t);
        }
      }
      dispatch (callback, result);
    }, () -> dispatch (callback, HistoryResult.serverOnly (messages)));
  }

  public void processHistory (
    TdApi.Message[] serverMessages,
    int direction,
    long fromMessageId,
    int limit,
    @Nullable TdApi.MessageTopic topicId,
    RunnableData<HistoryResult> callback
  ) {
    processHistory (
      findChatId (serverMessages), serverMessages, direction, fromMessageId,
      limit, topicId, callback
    );
  }

  public void markUserDelete (long chatId, long[] messageIds) {
    deletionTracker.mark (chatId, messageIds, System.currentTimeMillis ());
  }

  public void cancelUserDelete (long chatId, long[] messageIds) {
    deletionTracker.cancel (chatId, messageIds);
  }

  public void markUserClearChat (long chatId) {
    deletionTracker.markChat (chatId, System.currentTimeMillis ());
  }

  public void cancelUserClearChat (long chatId) {
    deletionTracker.clearChat (chatId);
  }

  public void processDeleteUpdate (
    TdApi.UpdateDeleteMessages update,
    RunnableData<DeletionResult> callback
  ) {
    long[] messageIds = Arrays.copyOf (update.messageIds, update.messageIds.length);
    post (() -> {
      long now = System.currentTimeMillis ();
      List<Long> retained = new ArrayList<> ();
      List<Long> removed = new ArrayList<> ();
      List<ArchivedSnapshot> mediaToArchive = new ArrayList<> ();
      try {
        boolean clearChat = deletionTracker.isChatMarked (update.chatId, now);
        for (long messageId : messageIds) {
          AyuMessageKey key = new AyuMessageKey (update.chatId, messageId);
          boolean userDelete = clearChat ||
            deletionTracker.consume (update.chatId, messageId, now);
          if (userDelete) {
            database.deleteMessages (update.chatId, new long[] {messageId});
            deletedKeys.remove (key);
            removed.add (messageId);
          } else if (!update.isPermanent) {
            database.deleteLiveMessages (update.chatId, new long[] {messageId});
            removed.add (messageId);
          } else if (Settings.instance ().saveDeletedMessages ()) {
            byte[] snapshot = database.markDeleted (update.chatId, messageId, now);
            if (snapshot != null) {
              deletedKeys.add (key);
              retained.add (messageId);
              mediaToArchive.add (
                new ArchivedSnapshot (update.chatId, messageId, snapshot)
              );
            } else {
              removed.add (messageId);
            }
          } else {
            database.deleteLiveMessages (update.chatId, new long[] {messageId});
            removed.add (messageId);
          }
        }
        dispatch (
          callback,
          new DeletionResult (toArray (retained), toArray (removed))
        );
        for (ArchivedSnapshot snapshot : mediaToArchive) {
          mediaThread.post (() -> archiveDownloadedMedia (snapshot), 0);
        }
      } catch (Throwable t) {
        disable ("Unable to process Ayu delete update", t);
        dispatch (callback, new DeletionResult (new long[0], messageIds));
      }
    }, () -> dispatch (callback, new DeletionResult (new long[0], messageIds)));
  }

  public boolean isDeleted (long chatId, long messageId) {
    return deletedKeys.contains (new AyuMessageKey (chatId, messageId));
  }

  public void deleteArchived (long chatId, long[] messageIds, @Nullable Runnable after) {
    long[] ids = Arrays.copyOf (messageIds, messageIds.length);
    post (() -> {
      boolean deleted = false;
      try {
        database.deleteMessages (chatId, ids);
        for (long messageId : ids) {
          deletedKeys.remove (new AyuMessageKey (chatId, messageId));
          FileUtils.delete (new File (new File (mediaDirectory, Long.toString (chatId)),
            Long.toString (messageId)), true);
        }
        deleted = true;
      } catch (Throwable t) {
        Log.e ("Unable to delete archived Ayu messages", t);
      }
      if (deleted) dispatchUi (after);
    });
  }

  public void deleteChat (long chatId, @Nullable Runnable after) {
    post (() -> {
      try {
        database.deleteChat (chatId);
        deletedKeys.removeIf (key -> key.chatId == chatId);
        FileUtils.delete (new File (mediaDirectory, Long.toString (chatId)), true);
      } catch (Throwable t) {
        Log.e ("Unable to delete Ayu chat data", t);
      }
      deletionTracker.clearChat (chatId);
      dispatchUi (after);
    }, () -> {
      deletionTracker.clearChat (chatId);
      dispatchUi (after);
    });
  }

  public boolean closeAndDelete () {
    if (!closed) {
      closed = true;
      CountDownLatch latch = new CountDownLatch (2);
      thread.post (() -> {
        if (database != null) {
          database.close ();
          database = null;
        }
        latch.countDown ();
        thread.quitLooper (true);
      }, 0);
      mediaThread.post (() -> {
        latch.countDown ();
        mediaThread.quitLooper (true);
      }, 0);
      try {
        latch.await ();
      } catch (InterruptedException e) {
        Thread.currentThread ().interrupt ();
        return false;
      }
    }
    boolean success = true;
    success = FileUtils.delete (new File (accountDirectory, "ayu.sqlite"), false) && success;
    success = FileUtils.delete (new File (accountDirectory, "ayu.sqlite-wal"), false) && success;
    success = FileUtils.delete (new File (accountDirectory, "ayu.sqlite-shm"), false) && success;
    success = FileUtils.delete (mediaDirectory, true) && success;
    return success;
  }

  private void captureHistory (TdApi.Message[] messages) throws IOException {
    List<TdApi.Message> liveMessages = new ArrayList<> (messages.length);
    List<byte[]> snapshots = new ArrayList<> (messages.length);
    for (TdApi.Message message : messages) {
      if (message != null && !deletedKeys.contains (
        new AyuMessageKey (message.chatId, message.id)
      )) {
        liveMessages.add (message);
        snapshots.add (TdApiObjectCodec.encode (message));
      }
    }
    database.upsertLiveBatch (
      liveMessages.toArray (new TdApi.Message[0]),
      snapshots.toArray (new byte[0][]),
      System.currentTimeMillis ()
    );
  }

  private void archiveDownloadedMedia (ArchivedSnapshot archived) {
    if (closed) return;
    try {
      TdApi.Message message = TdApiObjectCodec.decode (
        archived.snapshot, TdApi.Message.class
      );
      List<TdApi.File> files = TD.getFiles (message);
      if (files == null || files.isEmpty ()) return;
      tdlib.files ().syncFiles (files, 1000L);
      boolean changed = false;
      File targetDirectory = new File (
        new File (mediaDirectory, Long.toString (archived.chatId)),
        Long.toString (archived.messageId)
      );
      for (TdApi.File file : files) {
        if (closed) return;
        if (file == null || file.local == null ||
            !file.local.isDownloadingCompleted || file.local.path == null) {
          continue;
        }
        File source = new File (file.local.path);
        if (!source.isFile ()) continue;
        if ((!targetDirectory.exists () && !targetDirectory.mkdirs ()) ||
            !targetDirectory.isDirectory ()) {
          Log.w ("Unable to create Ayu media directory: %s", targetDirectory);
          return;
        }
        String name = source.getName ();
        File target = new File (
          targetDirectory,
          file.id + "-" + (name.isEmpty () ? "file" : name)
        );
        if (FileUtils.copy (source, target)) {
          file.local.path = target.getAbsolutePath ();
          changed = true;
        } else {
          Log.w ("Unable to archive Ayu media: %s", source);
        }
      }
      if (changed) {
        byte[] snapshot = TdApiObjectCodec.encode (message);
        post (() -> {
          try {
            database.replaceDeletedSnapshot (
              archived.chatId, archived.messageId, snapshot,
              System.currentTimeMillis ()
            );
          } catch (Throwable t) {
            disable ("Unable to update archived Ayu media paths", t);
          }
        });
      }
    } catch (Throwable t) {
      Log.w ("Unable to archive Ayu message media", t);
    }
  }

  private void post (Runnable runnable) {
    post (runnable, null);
  }

  private void post (Runnable runnable, @Nullable Runnable unavailable) {
    if (closed || !available) {
      if (unavailable != null) unavailable.run ();
      return;
    }
    thread.post (() -> {
      if (closed || !available) {
        if (unavailable != null) unavailable.run ();
      } else {
        try {
          ensureDatabase ();
          runnable.run ();
        } catch (Throwable t) {
          disable ("Unable to open Ayu message storage", t);
          if (unavailable != null) unavailable.run ();
        }
      }
    }, 0);
  }

  private void ensureDatabase () {
    if (database == null) {
      database = new AyuMessageDatabase (accountDirectory);
      deletedKeys.addAll (database.loadDeletedKeys ());
    }
  }

  private void disable (String message, Throwable error) {
    Log.e (message, error);
    available = false;
    if (database != null) {
      database.close ();
      database = null;
    }
  }

  private <T> void dispatch (RunnableData<T> callback, T result) {
    tdlib.runOnTdlibThread (() -> callback.runWithData (result));
  }

  private void dispatchUi (@Nullable Runnable runnable) {
    if (runnable != null) {
      tdlib.runOnUiThread (runnable);
    }
  }

  private static long findChatId (TdApi.Message[] messages) {
    if (messages == null) return 0;
    for (TdApi.Message message : messages) {
      if (message != null) return message.chatId;
    }
    return 0;
  }

  private static long findNewestMessageId (TdApi.Message[] messages) {
    long result = 0;
    boolean found = false;
    for (TdApi.Message message : messages) {
      if (message != null && (!found || Long.compare (message.id, result) > 0)) {
        found = true;
        result = message.id;
      }
    }
    return result;
  }

  private static long findOldestMessageId (TdApi.Message[] messages) {
    long result = 0;
    boolean found = false;
    for (TdApi.Message message : messages) {
      if (message != null && (!found || Long.compare (message.id, result) < 0)) {
        found = true;
        result = message.id;
      }
    }
    return result;
  }

  private static long[] toArray (List<Long> values) {
    long[] result = new long[values.size ()];
    for (int index = 0; index < values.size (); index++) {
      result[index] = values.get (index);
    }
    return result;
  }

  public static final class DeletionResult {
    public final long[] retainedIds;
    public final long[] removedIds;

    public DeletionResult (long[] retainedIds, long[] removedIds) {
      this.retainedIds = retainedIds;
      this.removedIds = removedIds;
    }
  }

  public static final class HistoryResult {
    public final TdApi.Message[] messages;
    public final int serverMessageCount;
    public final long serverNewestMessageId;
    public final long serverOldestMessageId;

    public HistoryResult (
      TdApi.Message[] messages,
      int serverMessageCount,
      long serverNewestMessageId,
      long serverOldestMessageId
    ) {
      this.messages = messages;
      this.serverMessageCount = serverMessageCount;
      this.serverNewestMessageId = serverNewestMessageId;
      this.serverOldestMessageId = serverOldestMessageId;
    }

    public static HistoryResult serverOnly (TdApi.Message[] messages) {
      return new HistoryResult (
        messages,
        messages.length,
        findNewestMessageId (messages),
        findOldestMessageId (messages)
      );
    }
  }

  private static final class ArchivedSnapshot {
    private final long chatId;
    private final long messageId;
    private final byte[] snapshot;

    private ArchivedSnapshot (long chatId, long messageId, byte[] snapshot) {
      this.chatId = chatId;
      this.messageId = messageId;
      this.snapshot = snapshot;
    }
  }
}
