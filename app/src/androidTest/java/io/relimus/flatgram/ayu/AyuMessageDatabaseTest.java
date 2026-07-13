package io.relimus.flatgram.ayu;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.drinkless.tdlib.TdApi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import me.vkryl.core.FileUtils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AyuMessageDatabaseTest {
  private File directory;
  private AyuMessageDatabase database;

  @Before
  public void setUp () {
    Context context = ApplicationProvider.getApplicationContext ();
    directory = new File (context.getCacheDir (), "ayu-db-test-" + System.nanoTime ());
    assertTrue (directory.mkdirs ());
    database = new AyuMessageDatabase (directory);
  }

  @After
  public void tearDown () {
    database.close ();
    FileUtils.delete (directory, true);
  }

  @Test
  public void deletedRowCannotBeOverwrittenByLiveSnapshot () {
    TdApi.Message first = message (10, "first");
    database.upsertLive (first, new byte[] {1}, 100);
    assertArrayEquals (new byte[] {1}, database.markDeleted (1, 10, 200));

    database.upsertLive (message (10, "replacement"), new byte[] {2}, 300);

    List<byte[]> rows = database.queryDeleted (1, AyuHistoryMerger.Window.all ());
    assertEquals (1, rows.size ());
    assertArrayEquals (new byte[] {1}, rows.get (0));
  }

  @Test
  public void liveRowCanBeUpdatedAndDeleted () throws Exception {
    TdApi.Message original = message (20, "first");
    database.upsertLive (original, TdApiObjectCodec.encode (original), 100);

    database.updateContent (
      1,
      20,
      new TdApi.MessageText (
        new TdApi.FormattedText ("updated", new TdApi.TextEntity[0]), null, null
      ),
      200
    );
    byte[] snapshot = database.markDeleted (1, 20, 300);

    assertNotNull (snapshot);
    TdApi.Message decoded = TdApiObjectCodec.decode (snapshot, TdApi.Message.class);
    assertEquals ("updated", ((TdApi.MessageText) decoded.content).text.text);
    database.deleteMessages (1, new long[] {20});
    assertTrue (database.queryDeleted (1, AyuHistoryMerger.Window.all ()).isEmpty ());
  }

  @Test
  public void markDeletedOnlyTransitionsLiveRows () {
    assertNull (database.markDeleted (1, 30, 100));

    TdApi.Message message = message (30, "text");
    database.upsertLive (message, new byte[] {3}, 200);
    assertArrayEquals (new byte[] {3}, database.markDeleted (1, 30, 300));
    assertNull (database.markDeleted (1, 30, 400));
  }

  @Test
  public void deletingLiveCandidateKeepsArchivedRow () {
    TdApi.Message message = message (40, "text");
    database.upsertLive (message, new byte[] {4}, 100);
    assertNotNull (database.markDeleted (1, 40, 200));

    database.deleteLiveMessages (1, new long[] {40});

    List<byte[]> rows = database.queryDeleted (1, AyuHistoryMerger.Window.all ());
    assertEquals (1, rows.size ());
    assertArrayEquals (new byte[] {4}, rows.get (0));
  }

  private static TdApi.Message message (long id, String text) {
    TdApi.Message message = new TdApi.Message ();
    message.id = id;
    message.chatId = 1;
    message.senderId = new TdApi.MessageSenderUser (7);
    message.date = 100;
    message.unreadReactions = new TdApi.UnreadReaction[0];
    message.content = new TdApi.MessageText (
      new TdApi.FormattedText (text, new TdApi.TextEntity[0]), null, null
    );
    return message;
  }
}
