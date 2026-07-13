package io.relimus.flatgram.ayu;

import org.drinkless.tdlib.TdApi;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TdApiObjectCodecTest {
  private static final int MAGIC = 0x41595531;
  private static final int VERSION = 1;

  private static final byte TYPE_BOOLEAN = 1;
  private static final byte TYPE_STRING = 8;
  private static final byte TYPE_BYTE_ARRAY = 9;
  private static final byte TYPE_ARRAY = 10;
  private static final byte TYPE_OBJECT = 11;

  @Test
  public void roundTripsNestedMessage () throws IOException {
    TdApi.Message message = new TdApi.Message ();
    message.id = 123456789L;
    message.chatId = -100987654321L;
    message.isOutgoing = true;
    message.unreadReactions = new TdApi.UnreadReaction[0];
    message.content = new TdApi.MessageText (
      new TdApi.FormattedText ("Ayu nested text", new TdApi.TextEntity[0]),
      null,
      null
    );

    TdApi.Message decoded = TdApiObjectCodec.decode (
      TdApiObjectCodec.encode (message),
      TdApi.Message.class
    );

    assertEquals (message.id, decoded.id);
    assertEquals (message.chatId, decoded.chatId);
    assertTrue (decoded.isOutgoing);
    assertNotNull (decoded.unreadReactions);
    assertEquals (0, decoded.unreadReactions.length);
    assertTrue (decoded.content instanceof TdApi.MessageText);
    TdApi.MessageText content = (TdApi.MessageText) decoded.content;
    assertEquals ("Ayu nested text", content.text.text);
    assertNotNull (content.text.entities);
    assertEquals (0, content.text.entities.length);
    assertNull (content.linkPreview);
  }

  @Test
  public void roundTripsNullFields () throws IOException {
    TdApi.FormattedText decoded = TdApiObjectCodec.decode (
      TdApiObjectCodec.encode (new TdApi.FormattedText (null, null)),
      TdApi.FormattedText.class
    );

    assertNull (decoded.text);
    assertNull (decoded.entities);
  }

  @Test
  public void roundTripsConcreteTdApiObjectArray () throws IOException {
    TdApi.Message first = new TdApi.Message ();
    first.id = 11L;
    TdApi.Message second = new TdApi.Message ();
    second.id = 22L;

    TdApi.Messages decoded = TdApiObjectCodec.decode (
      TdApiObjectCodec.encode (new TdApi.Messages (2, new TdApi.Message[] {first, second})),
      TdApi.Messages.class
    );

    assertEquals (2, decoded.totalCount);
    assertEquals (2, decoded.messages.length);
    assertEquals (11L, decoded.messages[0].id);
    assertEquals (22L, decoded.messages[1].id);
  }

  @Test
  public void roundTripsPolymorphicTdApiObjectArray () throws IOException {
    TdApi.ChatAvailableReactionsSome decoded = TdApiObjectCodec.decode (
      TdApiObjectCodec.encode (new TdApi.ChatAvailableReactionsSome (
        new TdApi.ReactionType[] {
          new TdApi.ReactionTypeEmoji ("one"),
          new TdApi.ReactionTypeEmoji ("two")
        },
        4
      )),
      TdApi.ChatAvailableReactionsSome.class
    );

    assertEquals (2, decoded.reactions.length);
    assertTrue (decoded.reactions[0] instanceof TdApi.ReactionTypeEmoji);
    assertEquals ("one", ((TdApi.ReactionTypeEmoji) decoded.reactions[0]).emoji);
    assertEquals ("two", ((TdApi.ReactionTypeEmoji) decoded.reactions[1]).emoji);
  }

  @Test
  public void roundTripsByteArray () throws IOException {
    byte[] bytes = new byte[] {0, 1, -1, Byte.MIN_VALUE, Byte.MAX_VALUE};

    TdApi.Data decoded = TdApiObjectCodec.decode (
      TdApiObjectCodec.encode (new TdApi.Data (bytes)),
      TdApi.Data.class
    );

    assertArrayEquals (bytes, decoded.data);
  }

  @Test
  public void roundTripsMultidimensionalPrimitiveArray () throws IOException {
    byte[][] hashes = new byte[][] {
      new byte[0],
      null,
      new byte[] {1, 2, 3}
    };

    TdApi.InputPassportElementErrorSourceTranslationFiles decoded =
      TdApiObjectCodec.decode (
        TdApiObjectCodec.encode (
          new TdApi.InputPassportElementErrorSourceTranslationFiles (hashes)
        ),
        TdApi.InputPassportElementErrorSourceTranslationFiles.class
      );

    assertEquals (3, decoded.fileHashes.length);
    assertArrayEquals (new byte[0], decoded.fileHashes[0]);
    assertNull (decoded.fileHashes[1]);
    assertArrayEquals (new byte[] {1, 2, 3}, decoded.fileHashes[2]);
  }

  @Test
  public void roundTripsStringLongerThanWriteUtfLimit () throws IOException {
    char[] characters = new char[70_000];
    Arrays.fill (characters, 'x');
    String text = new String (characters);

    TdApi.FormattedText decoded = TdApiObjectCodec.decode (
      TdApiObjectCodec.encode (new TdApi.FormattedText (text, new TdApi.TextEntity[0])),
      TdApi.FormattedText.class
    );

    assertEquals (text, decoded.text);
  }

  @Test
  public void rejectsMalformedUtf8StringOnEncode () {
    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.encode (new TdApi.FormattedText (
        String.valueOf (Character.MIN_HIGH_SURROGATE),
        new TdApi.TextEntity[0]
      ))
    );
  }

  @Test
  public void rejectsNullArguments () throws IOException {
    assertThrows (IOException.class, () -> TdApiObjectCodec.encode (null));
    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (null, TdApi.Data.class)
    );
    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (TdApiObjectCodec.encode (new TdApi.Data ()), null)
    );
  }

  @Test
  public void rejectsInvalidMagic () throws IOException {
    byte[] encoded = TdApiObjectCodec.encode (new TdApi.Data (new byte[] {1}));
    encoded[0] ^= 1;

    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (encoded, TdApi.Data.class)
    );
  }

  @Test
  public void rejectsUnsupportedVersion () throws IOException {
    byte[] encoded = TdApiObjectCodec.encode (new TdApi.Data (new byte[] {1}));
    ByteBuffer.wrap (encoded).putInt (Integer.BYTES, VERSION + 1);

    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (encoded, TdApi.Data.class)
    );
  }

  @Test
  public void rejectsTruncatedInput () throws IOException {
    byte[] encoded = TdApiObjectCodec.encode (new TdApi.Data (new byte[] {1, 2, 3}));

    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (
        Arrays.copyOf (encoded, encoded.length - 1),
        TdApi.Data.class
      )
    );
  }

  @Test
  public void rejectsMismatchedRootType () throws IOException {
    byte[] encoded = TdApiObjectCodec.encode (new TdApi.Data (new byte[] {1}));

    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (encoded, TdApi.Message.class)
    );
  }

  @Test
  public void rejectsNonGeneratedTdApiSubclass () {
    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.encode (new ExternalObject ())
    );
  }

  @Test
  public void rejectsInjectedClassName () throws IOException {
    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (
        rawObject ("java.lang.Runtime", 0),
        TdApi.Object.class
      )
    );
  }

  @Test
  public void rejectsUnknownTypeTag () throws IOException {
    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (
        rawDataObjectWithField ("data", (byte) 127, null),
        TdApi.Data.class
      )
    );
  }

  @Test
  public void skipsUnknownFields () throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
    try (DataOutputStream output = new DataOutputStream (bytes)) {
      writeHeaderAndClass (output, "Data", 2);
      writeString (output, "futureFlag");
      output.writeByte (TYPE_BOOLEAN);
      output.writeBoolean (true);
      writeString (output, "data");
      output.writeByte (TYPE_BYTE_ARRAY);
      output.writeInt (3);
      output.write (new byte[] {3, 2, 1});
    }

    TdApi.Data decoded = TdApiObjectCodec.decode (bytes.toByteArray (), TdApi.Data.class);

    assertArrayEquals (new byte[] {3, 2, 1}, decoded.data);
  }

  @Test
  public void skipsUnknownFutureTdApiObject () throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
    try (DataOutputStream output = new DataOutputStream (bytes)) {
      writeHeaderAndClass (output, "Data", 2);
      writeString (output, "futureObject");
      output.writeByte (TYPE_OBJECT);
      writeString (output, "FutureMessageSnapshot");
      output.writeInt (1);
      writeString (output, "payload");
      output.writeByte (TYPE_STRING);
      writeString (output, "future value");
      writeString (output, "data");
      output.writeByte (TYPE_BYTE_ARRAY);
      output.writeInt (2);
      output.write (new byte[] {4, 2});
    }

    TdApi.Data decoded = TdApiObjectCodec.decode (bytes.toByteArray (), TdApi.Data.class);

    assertArrayEquals (new byte[] {4, 2}, decoded.data);
  }

  @Test
  public void skipsUnknownFutureTdApiObjectsInArray () throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
    try (DataOutputStream output = new DataOutputStream (bytes)) {
      writeHeaderAndClass (output, "Data", 2);
      writeString (output, "futureObjects");
      output.writeByte (TYPE_ARRAY);
      output.writeInt (2);
      output.writeByte (TYPE_OBJECT);
      writeString (output, "FutureMessageSnapshot");
      output.writeInt (0);
      output.writeByte (TYPE_OBJECT);
      writeString (output, "FutureReactionSnapshot");
      output.writeInt (0);
      writeString (output, "data");
      output.writeByte (TYPE_BYTE_ARRAY);
      output.writeInt (1);
      output.writeByte (7);
    }

    TdApi.Data decoded = TdApiObjectCodec.decode (bytes.toByteArray (), TdApi.Data.class);

    assertArrayEquals (new byte[] {7}, decoded.data);
  }

  @Test
  public void rejectsTrailingBytes () throws IOException {
    byte[] encoded = TdApiObjectCodec.encode (new TdApi.Data (new byte[] {1}));

    assertThrows (
      IOException.class,
      () -> TdApiObjectCodec.decode (
        Arrays.copyOf (encoded, encoded.length + 1),
        TdApi.Data.class
      )
    );
  }

  @Test
  public void preservesBooleanFalseValue () throws IOException {
    TdApi.ReplyMarkupForceReply decoded = TdApiObjectCodec.decode (
      TdApiObjectCodec.encode (new TdApi.ReplyMarkupForceReply (false, null)),
      TdApi.ReplyMarkupForceReply.class
    );

    assertFalse (decoded.isPersonal);
  }

  private static byte[] rawObject (String className, int fieldCount) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
    try (DataOutputStream output = new DataOutputStream (bytes)) {
      writeHeaderAndClass (output, className, fieldCount);
    }
    return bytes.toByteArray ();
  }

  private static byte[] rawDataObjectWithField (
    String fieldName,
    byte tag,
    byte[] value
  ) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
    try (DataOutputStream output = new DataOutputStream (bytes)) {
      writeHeaderAndClass (output, "Data", 1);
      writeString (output, fieldName);
      output.writeByte (tag);
      if (value != null) {
        output.write (value);
      }
    }
    return bytes.toByteArray ();
  }

  private static void writeHeaderAndClass (
    DataOutputStream output,
    String className,
    int fieldCount
  ) throws IOException {
    output.writeInt (MAGIC);
    output.writeInt (VERSION);
    writeString (output, className);
    output.writeInt (fieldCount);
  }

  private static void writeString (DataOutputStream output, String value) throws IOException {
    byte[] bytes = value.getBytes (StandardCharsets.UTF_8);
    output.writeInt (bytes.length);
    output.write (bytes);
  }

  private static final class ExternalObject extends TdApi.Object {
    @Override
    public int getConstructor () {
      return 0;
    }
  }
}
