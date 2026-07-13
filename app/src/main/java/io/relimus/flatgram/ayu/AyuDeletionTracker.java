package io.relimus.flatgram.ayu;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks user-initiated deletes for the Ayu behavior based on NagramXF. */
public final class AyuDeletionTracker {
  private final long timeoutMillis;
  private final ConcurrentHashMap<AyuMessageKey, Long> messages = new ConcurrentHashMap<> ();
  private final ConcurrentHashMap<Long, Long> chats = new ConcurrentHashMap<> ();

  public AyuDeletionTracker (long timeoutMillis) {
    if (timeoutMillis <= 0L) {
      throw new IllegalArgumentException ("timeoutMillis must be positive");
    }
    this.timeoutMillis = timeoutMillis;
  }

  public void mark (long chatId, long[] messageIds, long now) {
    cleanupExpired (now);
    Objects.requireNonNull (messageIds, "messageIds");
    for (long messageId : messageIds) {
      messages.put (new AyuMessageKey (chatId, messageId), now);
    }
  }

  public void cancel (long chatId, long[] messageIds) {
    Objects.requireNonNull (messageIds, "messageIds");
    for (long messageId : messageIds) {
      messages.remove (new AyuMessageKey (chatId, messageId));
    }
  }

  public boolean consume (long chatId, long messageId, long now) {
    cleanupExpired (now);
    AyuMessageKey key = new AyuMessageKey (chatId, messageId);
    Long markedAt = messages.get (key);
    return markedAt != null && messages.remove (key, markedAt);
  }

  public void markChat (long chatId, long now) {
    cleanupExpired (now);
    chats.put (chatId, now);
  }

  public boolean isChatMarked (long chatId, long now) {
    cleanupExpired (now);
    return chats.containsKey (chatId);
  }

  public void clearChat (long chatId) {
    chats.remove (chatId);
  }

  private void cleanupExpired (long now) {
    messages.forEach ((key, markedAt) -> {
      if (isExpired (markedAt, now)) {
        messages.remove (key, markedAt);
      }
    });
    chats.forEach ((chatId, markedAt) -> {
      if (isExpired (markedAt, now)) {
        chats.remove (chatId, markedAt);
      }
    });
  }

  private boolean isExpired (long markedAt, long now) {
    if (markedAt >= now) return false;
    try {
      return Math.subtractExact (now, markedAt) > timeoutMillis;
    } catch (ArithmeticException ignored) {
      return true;
    }
  }
}
