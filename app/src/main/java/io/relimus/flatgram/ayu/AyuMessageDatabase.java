package io.relimus.flatgram.ayu;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Account-local SQLite storage for the Ayu behavior based on NagramXF. */
public final class AyuMessageDatabase {
  private static final int DATABASE_VERSION = 1;
  private static final int SNAPSHOT_VERSION = 1;
  private static final int STATE_LIVE = 0;
  private static final int STATE_DELETED = 1;

  private static final String CREATE_MESSAGES =
    "CREATE TABLE ayu_messages (" +
      "chat_id INTEGER NOT NULL," +
      "message_id INTEGER NOT NULL," +
      "message_date INTEGER NOT NULL," +
      "state INTEGER NOT NULL," +
      "snapshot_version INTEGER NOT NULL," +
      "snapshot BLOB NOT NULL," +
      "updated_at INTEGER NOT NULL," +
      "deleted_at INTEGER," +
      "PRIMARY KEY(chat_id, message_id))";
  private static final String CREATE_MESSAGES_INDEX =
    "CREATE INDEX ayu_messages_chat_state_id " +
      "ON ayu_messages(chat_id, state, message_id)";

  private final SQLiteDatabase database;

  public AyuMessageDatabase (File directory) {
    if ((!directory.exists () && !directory.mkdirs ()) || !directory.isDirectory ()) {
      throw new SQLiteException ("Unable to create Ayu database directory: " + directory);
    }
    database = SQLiteDatabase.openOrCreateDatabase (new File (directory, "ayu.sqlite"), null);
    try {
      database.enableWriteAheadLogging ();
      int version = database.getVersion ();
      if (version > DATABASE_VERSION) {
        throw new SQLiteException ("Unsupported Ayu database version: " + version);
      }
      if (version == 0) {
        database.beginTransaction ();
        try {
          database.execSQL (CREATE_MESSAGES);
          database.execSQL (CREATE_MESSAGES_INDEX);
          database.setVersion (DATABASE_VERSION);
          database.setTransactionSuccessful ();
        } finally {
          database.endTransaction ();
        }
      }
    } catch (RuntimeException e) {
      database.close ();
      throw e;
    }
  }

  public void upsertLive (TdApi.Message message, byte[] snapshot, long now) {
    ContentValues values = liveValues (message, snapshot, now);
    int updated = database.update (
      "ayu_messages",
      values,
      "chat_id = ? AND message_id = ? AND state = ?",
      args (message.chatId, message.id, STATE_LIVE)
    );
    if (updated == 0) {
      database.insertWithOnConflict (
        "ayu_messages",
        null,
        values,
        SQLiteDatabase.CONFLICT_IGNORE
      );
    }
  }

  public void upsertLiveBatch (
    TdApi.Message[] messages,
    byte[][] snapshots,
    long now
  ) {
    if (messages.length != snapshots.length) {
      throw new IllegalArgumentException ("Message and snapshot counts differ");
    }
    database.beginTransaction ();
    try {
      for (int index = 0; index < messages.length; index++) {
        upsertLive (messages[index], snapshots[index], now);
      }
      database.setTransactionSuccessful ();
    } finally {
      database.endTransaction ();
    }
  }

  public void updateContent (
    long chatId,
    long messageId,
    TdApi.MessageContent content,
    long now
  ) {
    updateLiveSnapshot (chatId, messageId, now, message -> message.content = content);
  }

  public void updateEdited (
    long chatId,
    long messageId,
    int editDate,
    @Nullable TdApi.ReplyMarkup replyMarkup,
    long now
  ) {
    updateLiveSnapshot (chatId, messageId, now, message -> {
      message.editDate = editDate;
      message.replyMarkup = replyMarkup;
    });
  }

  public @Nullable byte[] markDeleted (long chatId, long messageId, long now) {
    database.beginTransaction ();
    try {
      byte[] snapshot = findLiveSnapshot (chatId, messageId);
      if (snapshot == null) {
        database.setTransactionSuccessful ();
        return null;
      }
      ContentValues values = new ContentValues ();
      values.put ("state", STATE_DELETED);
      values.put ("updated_at", now);
      values.put ("deleted_at", now);
      int updated = database.update (
        "ayu_messages",
        values,
        "chat_id = ? AND message_id = ? AND state = ?",
        args (chatId, messageId, STATE_LIVE)
      );
      database.setTransactionSuccessful ();
      return updated == 1 ? snapshot : null;
    } finally {
      database.endTransaction ();
    }
  }

  public List<byte[]> queryDeleted (long chatId, AyuHistoryMerger.Window window) {
    StringBuilder selection = new StringBuilder ("chat_id = ? AND state = ?");
    List<String> arguments = new ArrayList<> ();
    arguments.add (Long.toString (chatId));
    arguments.add (Integer.toString (STATE_DELETED));
    if (window.hasLowerBound) {
      selection.append (window.lowerInclusive ? " AND message_id >= ?" : " AND message_id > ?");
      arguments.add (Long.toString (window.lowerBound));
    }
    if (window.hasUpperBound) {
      selection.append (window.upperInclusive ? " AND message_id <= ?" : " AND message_id < ?");
      arguments.add (Long.toString (window.upperBound));
    }

    List<byte[]> snapshots = new ArrayList<> ();
    try (Cursor cursor = database.query (
      "ayu_messages",
      new String[] {"snapshot"},
      selection.toString (),
      arguments.toArray (new String[0]),
      null,
      null,
      "message_id " + (window.ascending ? "ASC" : "DESC"),
      window.sqlLimit > 0 ? Integer.toString (window.sqlLimit) : null
    )) {
      while (cursor.moveToNext ()) {
        snapshots.add (cursor.getBlob (0));
      }
    }
    return snapshots;
  }

  public void replaceDeletedSnapshot (
    long chatId,
    long messageId,
    byte[] snapshot,
    long now
  ) {
    ContentValues values = new ContentValues ();
    values.put ("snapshot_version", SNAPSHOT_VERSION);
    values.put ("snapshot", snapshot);
    values.put ("updated_at", now);
    database.update (
      "ayu_messages",
      values,
      "chat_id = ? AND message_id = ? AND state = ?",
      args (chatId, messageId, STATE_DELETED)
    );
  }

  public void deleteMessages (long chatId, long[] messageIds) {
    deleteMessages (chatId, messageIds, false);
  }

  public void deleteLiveMessages (long chatId, long[] messageIds) {
    deleteMessages (chatId, messageIds, true);
  }

  private void deleteMessages (long chatId, long[] messageIds, boolean onlyLive) {
    if (messageIds.length == 0) return;
    StringBuilder selection = new StringBuilder ("chat_id = ?");
    String[] arguments = new String[messageIds.length + (onlyLive ? 2 : 1)];
    arguments[0] = Long.toString (chatId);
    int argumentOffset = 1;
    if (onlyLive) {
      selection.append (" AND state = ?");
      arguments[1] = Integer.toString (STATE_LIVE);
      argumentOffset++;
    }
    selection.append (" AND message_id IN (");
    for (int index = 0; index < messageIds.length; index++) {
      if (index > 0) selection.append (',');
      selection.append ('?');
      arguments[index + argumentOffset] = Long.toString (messageIds[index]);
    }
    selection.append (')');
    database.delete ("ayu_messages", selection.toString (), arguments);
  }

  public void deleteChat (long chatId) {
    database.delete ("ayu_messages", "chat_id = ?", args (chatId));
  }

  public Set<AyuMessageKey> loadDeletedKeys () {
    Set<AyuMessageKey> keys = new HashSet<> ();
    try (Cursor cursor = database.query (
      "ayu_messages",
      new String[] {"chat_id", "message_id"},
      "state = ?",
      new String[] {Integer.toString (STATE_DELETED)},
      null,
      null,
      null
    )) {
      while (cursor.moveToNext ()) {
        keys.add (new AyuMessageKey (cursor.getLong (0), cursor.getLong (1)));
      }
    }
    return keys;
  }

  public void close () {
    if (database.isOpen ()) {
      database.close ();
    }
  }

  private void updateLiveSnapshot (
    long chatId,
    long messageId,
    long now,
    MessageUpdater updater
  ) {
    byte[] snapshot = findLiveSnapshot (chatId, messageId);
    if (snapshot == null) return;
    try {
      TdApi.Message message = TdApiObjectCodec.decode (snapshot, TdApi.Message.class);
      updater.update (message);
      ContentValues values = new ContentValues ();
      values.put ("snapshot_version", SNAPSHOT_VERSION);
      values.put ("snapshot", TdApiObjectCodec.encode (message));
      values.put ("updated_at", now);
      database.update (
        "ayu_messages",
        values,
        "chat_id = ? AND message_id = ? AND state = ?",
        args (chatId, messageId, STATE_LIVE)
      );
    } catch (IOException e) {
      throw new SQLiteException ("Unable to update Ayu message snapshot", e);
    }
  }

  private @Nullable byte[] findLiveSnapshot (long chatId, long messageId) {
    try (Cursor cursor = database.query (
      "ayu_messages",
      new String[] {"snapshot"},
      "chat_id = ? AND message_id = ? AND state = ?",
      args (chatId, messageId, STATE_LIVE),
      null,
      null,
      null,
      "1"
    )) {
      return cursor.moveToFirst () ? cursor.getBlob (0) : null;
    }
  }

  private static ContentValues liveValues (
    TdApi.Message message,
    byte[] snapshot,
    long now
  ) {
    ContentValues values = new ContentValues ();
    values.put ("chat_id", message.chatId);
    values.put ("message_id", message.id);
    values.put ("message_date", message.date);
    values.put ("state", STATE_LIVE);
    values.put ("snapshot_version", SNAPSHOT_VERSION);
    values.put ("snapshot", snapshot);
    values.put ("updated_at", now);
    values.putNull ("deleted_at");
    return values;
  }

  private static String[] args (long first, long second, int third) {
    return new String[] {
      Long.toString (first), Long.toString (second), Integer.toString (third)
    };
  }

  private static String[] args (long value) {
    return new String[] {Long.toString (value)};
  }

  private interface MessageUpdater {
    void update (TdApi.Message message);
  }
}
