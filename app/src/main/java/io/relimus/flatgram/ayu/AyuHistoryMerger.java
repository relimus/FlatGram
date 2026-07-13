package io.relimus.flatgram.ayu;

import org.drinkless.tdlib.TdApi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Resolves and merges deleted-message history windows for the Ayu behavior. */
public final class AyuHistoryMerger {
  public static final int DIRECTION_INITIAL = 0;
  public static final int DIRECTION_OLDER = 1;
  public static final int DIRECTION_NEWER = 2;

  private AyuHistoryMerger () { }

  public static Window resolveWindow (
    int direction,
    long fromMessageId,
    int limit,
    TdApi.Message[] serverMessages
  ) {
    if (
      direction != DIRECTION_INITIAL &&
      direction != DIRECTION_OLDER &&
      direction != DIRECTION_NEWER
    ) {
      throw new IllegalArgumentException ("Unknown history direction: " + direction);
    }
    if (limit < 0) {
      throw new IllegalArgumentException ("limit must not be negative");
    }
    Objects.requireNonNull (serverMessages, "serverMessages");

    boolean hasServerMessage = false;
    long oldestServerId = 0L;
    long newestServerId = 0L;
    for (TdApi.Message message : serverMessages) {
      if (message == null) continue;
      if (!hasServerMessage) {
        hasServerMessage = true;
        oldestServerId = message.id;
        newestServerId = message.id;
      } else {
        if (Long.compare (message.id, oldestServerId) < 0) {
          oldestServerId = message.id;
        }
        if (Long.compare (message.id, newestServerId) > 0) {
          newestServerId = message.id;
        }
      }
    }

    if (!hasServerMessage) {
      switch (direction) {
        case DIRECTION_INITIAL:
          return new Window (false, 0L, false, false, 0L, false, false, limit);
        case DIRECTION_OLDER:
          return new Window (
            false, 0L, false, true, fromMessageId, false, false, limit
          );
        case DIRECTION_NEWER:
          return new Window (
            true, fromMessageId, false, false, 0L, false, true, limit
          );
        default:
          throw new AssertionError (direction);
      }
    }

    switch (direction) {
      case DIRECTION_INITIAL:
        return new Window (
          true, oldestServerId, true, false, 0L, false, false, Window.NO_LIMIT
        );
      case DIRECTION_OLDER:
        return new Window (
          true,
          oldestServerId,
          true,
          true,
          fromMessageId,
          false,
          false,
          Window.NO_LIMIT
        );
      case DIRECTION_NEWER:
        return new Window (
          true,
          fromMessageId,
          false,
          true,
          newestServerId,
          true,
          true,
          Window.NO_LIMIT
        );
      default:
        throw new AssertionError (direction);
    }
  }

  public static TdApi.Message[] merge (
    TdApi.Message[] serverMessages,
    List<TdApi.Message> deletedMessages
  ) {
    Objects.requireNonNull (serverMessages, "serverMessages");
    Objects.requireNonNull (deletedMessages, "deletedMessages");

    Map<Long, TdApi.Message> messagesById = new HashMap<> ();
    for (TdApi.Message message : serverMessages) {
      if (message != null) {
        messagesById.putIfAbsent (message.id, message);
      }
    }
    for (TdApi.Message message : deletedMessages) {
      if (message != null) {
        messagesById.putIfAbsent (message.id, message);
      }
    }

    TdApi.Message[] merged = messagesById.values ().toArray (new TdApi.Message[0]);
    Arrays.sort (merged, (first, second) -> Long.compare (second.id, first.id));
    return merged;
  }

  public static final class Window {
    public static final int NO_LIMIT = 0;

    private static final Window ALL = new Window (
      false, 0L, false, false, 0L, false, false, NO_LIMIT
    );

    public final boolean hasLowerBound;
    public final long lowerBound;
    public final boolean lowerInclusive;
    public final boolean hasUpperBound;
    public final long upperBound;
    public final boolean upperInclusive;
    public final boolean ascending;
    public final int sqlLimit;

    private Window (
      boolean hasLowerBound,
      long lowerBound,
      boolean lowerInclusive,
      boolean hasUpperBound,
      long upperBound,
      boolean upperInclusive,
      boolean ascending,
      int sqlLimit
    ) {
      this.hasLowerBound = hasLowerBound;
      this.lowerBound = lowerBound;
      this.lowerInclusive = lowerInclusive;
      this.hasUpperBound = hasUpperBound;
      this.upperBound = upperBound;
      this.upperInclusive = upperInclusive;
      this.ascending = ascending;
      this.sqlLimit = sqlLimit;
    }

    public static Window all () {
      return ALL;
    }

    public boolean hasLowerBound () {
      return hasLowerBound;
    }

    public long getLowerBound () {
      return lowerBound;
    }

    public boolean isLowerInclusive () {
      return lowerInclusive;
    }

    public boolean hasUpperBound () {
      return hasUpperBound;
    }

    public long getUpperBound () {
      return upperBound;
    }

    public boolean isUpperInclusive () {
      return upperInclusive;
    }

    public boolean isAscending () {
      return ascending;
    }

    public int getSqlLimit () {
      return sqlLimit;
    }
  }
}
