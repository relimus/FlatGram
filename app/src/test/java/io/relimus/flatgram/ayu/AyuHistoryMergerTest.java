package io.relimus.flatgram.ayu;

import org.drinkless.tdlib.TdApi;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AyuHistoryMergerTest {
  @Test
  public void mergesByDescendingIdAndKeepsServerInstance () {
    TdApi.Message serverDuplicate = message (100L);
    TdApi.Message archivedDuplicate = message (100L);

    TdApi.Message[] merged = AyuHistoryMerger.merge (
      new TdApi.Message[] {message (105L), serverDuplicate},
      Arrays.asList (message (110L), message (103L), archivedDuplicate)
    );

    assertArrayEquals (new long[] {110L, 105L, 103L, 100L}, ids (merged));
    assertSame (serverDuplicate, merged[3]);
  }

  @Test
  public void removesDuplicatesWithinEachSourceWithoutMutatingInputs () {
    TdApi.Message firstServer = message (105L);
    TdApi.Message secondServer = message (105L);
    TdApi.Message[] server = new TdApi.Message[] {
      message (100L), firstServer, secondServer
    };
    TdApi.Message firstArchived = message (110L);
    TdApi.Message secondArchived = message (110L);
    List<TdApi.Message> archived = new ArrayList<> (Arrays.asList (
      message (103L), firstArchived, secondArchived, message (103L)
    ));

    TdApi.Message[] merged = AyuHistoryMerger.merge (server, archived);

    assertArrayEquals (new long[] {110L, 105L, 103L, 100L}, ids (merged));
    assertSame (firstArchived, merged[0]);
    assertSame (firstServer, merged[1]);
    assertArrayEquals (new long[] {100L, 105L, 105L}, ids (server));
    assertArrayEquals (new long[] {103L, 110L, 110L, 103L}, ids (archived));
  }

  @Test
  public void ignoresNullMessagesAndRejectsNullSources () {
    TdApi.Message[] server = new TdApi.Message[] {null, message (100L)};
    List<TdApi.Message> archived = Arrays.asList (message (110L), null);

    assertArrayEquals (
      new long[] {110L, 100L},
      ids (AyuHistoryMerger.merge (server, archived))
    );
    assertNull (server[0]);
    assertNull (archived.get (1));
    assertThrows (NullPointerException.class, () -> AyuHistoryMerger.merge (null, archived));
    assertThrows (NullPointerException.class, () -> AyuHistoryMerger.merge (server, null));
  }

  @Test
  public void sortsLongIdExtremesWithoutOverflow () {
    TdApi.Message[] merged = AyuHistoryMerger.merge (
      new TdApi.Message[] {message (Long.MIN_VALUE), message (0L)},
      Arrays.asList (message (Long.MAX_VALUE), message (-1L))
    );

    assertArrayEquals (
      new long[] {Long.MAX_VALUE, 0L, -1L, Long.MIN_VALUE},
      ids (merged)
    );
  }

  @Test
  public void resolvesInitialWindowFromOldestServerMessage () {
    AyuHistoryMerger.Window window = AyuHistoryMerger.resolveWindow (
      AyuHistoryMerger.DIRECTION_INITIAL,
      0L,
      50,
      messages (103L, 100L, 105L)
    );

    assertWindow (window, true, 100L, true, false, 0L, false, false, 0);
  }

  @Test
  public void resolvesOlderWindowWithExclusiveFromMessageId () {
    AyuHistoryMerger.Window window = AyuHistoryMerger.resolveWindow (
      AyuHistoryMerger.DIRECTION_OLDER,
      120L,
      50,
      messages (103L, 100L, 105L)
    );

    assertWindow (window, true, 100L, true, true, 120L, false, false, 0);
  }

  @Test
  public void resolvesNewerWindowWithExclusiveFromMessageId () {
    AyuHistoryMerger.Window window = AyuHistoryMerger.resolveWindow (
      AyuHistoryMerger.DIRECTION_NEWER,
      90L,
      50,
      messages (103L, 100L, 105L)
    );

    assertWindow (window, true, 90L, false, true, 105L, true, true, 0);
  }

  @Test
  public void resolvesLimitedWindowsForEmptyServerResponse () {
    assertWindow (
      AyuHistoryMerger.resolveWindow (
        AyuHistoryMerger.DIRECTION_INITIAL,
        0L,
        50,
        new TdApi.Message[0]
      ),
      false, 0L, false, false, 0L, false, false, 50
    );
    assertWindow (
      AyuHistoryMerger.resolveWindow (
        AyuHistoryMerger.DIRECTION_OLDER,
        120L,
        40,
        new TdApi.Message[] {null}
      ),
      false, 0L, false, true, 120L, false, false, 40
    );
    assertWindow (
      AyuHistoryMerger.resolveWindow (
        AyuHistoryMerger.DIRECTION_NEWER,
        120L,
        30,
        new TdApi.Message[0]
      ),
      true, 120L, false, false, 0L, false, true, 30
    );
  }

  @Test
  public void allWindowIsUnboundedAndUnlimited () {
    assertWindow (
      AyuHistoryMerger.Window.all (),
      false, 0L, false, false, 0L, false, false, 0
    );
  }

  @Test
  public void preservesLongBoundsWithoutAdjustingIds () {
    AyuHistoryMerger.Window older = AyuHistoryMerger.resolveWindow (
      AyuHistoryMerger.DIRECTION_OLDER,
      Long.MAX_VALUE,
      10,
      messages (Long.MAX_VALUE, Long.MIN_VALUE)
    );
    AyuHistoryMerger.Window newer = AyuHistoryMerger.resolveWindow (
      AyuHistoryMerger.DIRECTION_NEWER,
      Long.MIN_VALUE,
      10,
      messages (Long.MAX_VALUE, Long.MIN_VALUE)
    );

    assertWindow (
      older,
      true, Long.MIN_VALUE, true, true, Long.MAX_VALUE, false, false, 0
    );
    assertWindow (
      newer,
      true, Long.MIN_VALUE, false, true, Long.MAX_VALUE, true, true, 0
    );
  }

  @Test
  public void rejectsInvalidDirectionLimitAndNullServerArray () {
    assertThrows (
      IllegalArgumentException.class,
      () -> AyuHistoryMerger.resolveWindow (-1, 0L, 10, new TdApi.Message[0])
    );
    assertThrows (
      IllegalArgumentException.class,
      () -> AyuHistoryMerger.resolveWindow (3, 0L, 10, new TdApi.Message[0])
    );
    assertThrows (
      IllegalArgumentException.class,
      () -> AyuHistoryMerger.resolveWindow (
        AyuHistoryMerger.DIRECTION_INITIAL,
        0L,
        -1,
        new TdApi.Message[0]
      )
    );
    assertThrows (
      NullPointerException.class,
      () -> AyuHistoryMerger.resolveWindow (
        AyuHistoryMerger.DIRECTION_INITIAL,
        0L,
        10,
        null
      )
    );
  }

  private static void assertWindow (
    AyuHistoryMerger.Window window,
    boolean hasLowerBound,
    long lowerBound,
    boolean lowerInclusive,
    boolean hasUpperBound,
    long upperBound,
    boolean upperInclusive,
    boolean ascending,
    int sqlLimit
  ) {
    assertEquals (hasLowerBound, window.hasLowerBound);
    assertEquals (lowerInclusive, window.lowerInclusive);
    assertEquals (hasUpperBound, window.hasUpperBound);
    assertEquals (upperInclusive, window.upperInclusive);
    assertEquals (ascending, window.ascending);
    assertEquals (sqlLimit, window.sqlLimit);
    if (hasLowerBound) {
      assertEquals (lowerBound, window.lowerBound);
    }
    if (hasUpperBound) {
      assertEquals (upperBound, window.upperBound);
    }
  }

  private static TdApi.Message[] messages (long... ids) {
    TdApi.Message[] messages = new TdApi.Message[ids.length];
    for (int index = 0; index < ids.length; index++) {
      messages[index] = message (ids[index]);
    }
    return messages;
  }

  private static TdApi.Message message (long id) {
    TdApi.Message message = new TdApi.Message ();
    message.id = id;
    return message;
  }

  private static long[] ids (TdApi.Message[] messages) {
    long[] ids = new long[messages.length];
    for (int index = 0; index < messages.length; index++) {
      ids[index] = messages[index].id;
    }
    return ids;
  }

  private static long[] ids (List<TdApi.Message> messages) {
    return ids (messages.toArray (new TdApi.Message[0]));
  }
}
