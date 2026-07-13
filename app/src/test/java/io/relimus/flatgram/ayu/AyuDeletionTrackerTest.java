package io.relimus.flatgram.ayu;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AyuDeletionTrackerTest {
  @Test
  public void consumesMarkedMessageOnlyOnceAndKeepsChatsIsolated () {
    AyuDeletionTracker tracker = new AyuDeletionTracker (30_000L);
    tracker.mark (10L, new long[] {1L, 2L}, 1_000L);

    assertFalse (tracker.consume (11L, 1L, 2_000L));
    assertTrue (tracker.consume (10L, 1L, 2_000L));
    assertFalse (tracker.consume (10L, 1L, 2_000L));
    assertTrue (tracker.consume (10L, 2L, 2_000L));
  }

  @Test
  public void expiresMessagesAfterTimeoutButNotAtBoundary () {
    AyuDeletionTracker tracker = new AyuDeletionTracker (30_000L);
    tracker.mark (10L, new long[] {1L, 2L}, 1_000L);

    assertTrue (tracker.consume (10L, 1L, 31_000L));
    assertFalse (tracker.consume (10L, 2L, 31_001L));
  }

  @Test
  public void keepsChatMarkedAcrossQueriesUntilCleared () {
    AyuDeletionTracker tracker = new AyuDeletionTracker (30_000L);
    tracker.markChat (10L, 1_000L);

    assertTrue (tracker.isChatMarked (10L, 2_000L));
    assertTrue (tracker.isChatMarked (10L, 3_000L));

    tracker.clearChat (10L);

    assertFalse (tracker.isChatMarked (10L, 3_001L));
  }

  @Test
  public void cancelsOnlyRequestedMessages () {
    AyuDeletionTracker tracker = new AyuDeletionTracker (30_000L);
    tracker.mark (10L, new long[] {1L, 2L}, 1_000L);
    tracker.mark (11L, new long[] {1L}, 1_000L);

    tracker.cancel (10L, new long[] {1L});

    assertFalse (tracker.consume (10L, 1L, 2_000L));
    assertTrue (tracker.consume (10L, 2L, 2_000L));
    assertTrue (tracker.consume (11L, 1L, 2_000L));
  }

  @Test
  public void expiresChatMarkersAndKeepsChatsIsolated () {
    AyuDeletionTracker tracker = new AyuDeletionTracker (30_000L);
    tracker.markChat (10L, 1_000L);
    tracker.markChat (11L, 2_000L);

    assertFalse (tracker.isChatMarked (10L, 31_001L));
    assertTrue (tracker.isChatMarked (11L, 31_001L));

    tracker.clearChat (11L);

    assertFalse (tracker.isChatMarked (11L, 31_001L));
  }

  @Test
  public void consumesMessageAtomically () throws Exception {
    AyuDeletionTracker tracker = new AyuDeletionTracker (30_000L);
    tracker.mark (10L, new long[] {1L}, 1_000L);
    ExecutorService executor = Executors.newFixedThreadPool (2);
    CountDownLatch ready = new CountDownLatch (2);
    CountDownLatch start = new CountDownLatch (1);
    try {
      Future<Boolean> first = executor.submit (() -> {
        ready.countDown ();
        start.await ();
        return tracker.consume (10L, 1L, 2_000L);
      });
      Future<Boolean> second = executor.submit (() -> {
        ready.countDown ();
        start.await ();
        return tracker.consume (10L, 1L, 2_000L);
      });
      assertTrue (ready.await (5L, TimeUnit.SECONDS));

      start.countDown ();

      assertNotEquals (first.get (5L, TimeUnit.SECONDS), second.get (5L, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow ();
    }
  }

  @Test
  public void acceptsEmptyMessageArraysAndRejectsNullArrays () {
    AyuDeletionTracker tracker = new AyuDeletionTracker (30_000L);

    tracker.mark (10L, new long[0], 1_000L);
    tracker.cancel (10L, new long[0]);

    assertThrows (NullPointerException.class, () -> tracker.mark (10L, null, 1_000L));
    assertThrows (NullPointerException.class, () -> tracker.cancel (10L, null));
  }

  @Test
  public void rejectsNonPositiveTimeout () {
    assertThrows (IllegalArgumentException.class, () -> new AyuDeletionTracker (0L));
    assertThrows (IllegalArgumentException.class, () -> new AyuDeletionTracker (-1L));
  }

  @Test
  public void messageKeyUsesBothIdentifiersForEquality () {
    AyuMessageKey key = new AyuMessageKey (10L, 1L);
    Set<AyuMessageKey> keys = new HashSet<> ();
    keys.add (key);
    keys.add (new AyuMessageKey (10L, 1L));
    keys.add (new AyuMessageKey (10L, 2L));
    keys.add (new AyuMessageKey (11L, 1L));

    assertEquals (10L, key.chatId);
    assertEquals (1L, key.messageId);
    assertEquals (3, keys.size ());
    assertNotEquals (key, new AyuMessageKey (10L, 2L));
    assertNotEquals (key, new AyuMessageKey (11L, 1L));
  }
}
