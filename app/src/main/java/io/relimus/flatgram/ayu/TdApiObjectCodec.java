package io.relimus.flatgram.ayu;

import org.drinkless.tdlib.TdApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Versioned TDLib snapshot codec for the Ayu behavior based on NagramXF. */
public final class TdApiObjectCodec {
  private static final int MAGIC = 0x41595531;
  private static final int VERSION = 1;

  private static final byte TYPE_NULL = 0;
  private static final byte TYPE_BOOLEAN = 1;
  private static final byte TYPE_BYTE = 2;
  private static final byte TYPE_SHORT = 3;
  private static final byte TYPE_INT = 4;
  private static final byte TYPE_LONG = 5;
  private static final byte TYPE_FLOAT = 6;
  private static final byte TYPE_DOUBLE = 7;
  private static final byte TYPE_STRING = 8;
  private static final byte TYPE_BYTE_ARRAY = 9;
  private static final byte TYPE_ARRAY = 10;
  private static final byte TYPE_OBJECT = 11;

  private static final int MAX_INPUT_BYTES = 64 * 1024 * 1024;
  private static final int MAX_STRING_BYTES = 16 * 1024 * 1024;
  private static final int MAX_BYTE_ARRAY_LENGTH = 32 * 1024 * 1024;
  private static final int MAX_ARRAY_LENGTH = 1_000_000;
  private static final int MAX_FIELD_COUNT = 4_096;
  private static final int MAX_CLASS_NAME_BYTES = 128;
  private static final int MAX_FIELD_NAME_BYTES = 256;
  private static final int MAX_DEPTH = 64;

  private static final String TD_API_CLASS_PREFIX = TdApi.class.getName () + "$";

  private TdApiObjectCodec () { }

  public static byte[] encode (TdApi.Object object) throws IOException {
    if (object == null) {
      throw new IOException ("Root object must not be null");
    }
    validateTdApiClass (object.getClass (), false);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream ();
    try (DataOutputStream output = new DataOutputStream (
      new LimitedOutputStream (bytes, MAX_INPUT_BYTES)
    )) {
      output.writeInt (MAGIC);
      output.writeInt (VERSION);
      writeObject (output, object, 0);
    }
    return bytes.toByteArray ();
  }

  public static <T extends TdApi.Object> T decode (
    byte[] data,
    Class<T> expectedType
  ) throws IOException {
    if (data == null) {
      throw new IOException ("Encoded data must not be null");
    }
    if (expectedType == null) {
      throw new IOException ("Expected type must not be null");
    }
    if (data.length > MAX_INPUT_BYTES) {
      throw new IOException ("Encoded data is too large: " + data.length);
    }
    validateTdApiClass (expectedType, true);

    ByteArrayInputStream bytes = new ByteArrayInputStream (data);
    try (DataInputStream input = new DataInputStream (bytes)) {
      int magic = input.readInt ();
      if (magic != MAGIC) {
        throw new IOException ("Invalid codec magic: " + Integer.toHexString (magic));
      }
      int version = input.readInt ();
      if (version != VERSION) {
        throw new IOException ("Unsupported codec version: " + version);
      }

      T object = expectedType.cast (readObject (input, expectedType, 0));
      if (input.available () != 0) {
        throw new IOException ("Unexpected trailing data: " + input.available () + " bytes");
      }
      return object;
    }
  }

  private static void writeObject (
    DataOutput output,
    TdApi.Object object,
    int depth
  ) throws IOException {
    checkDepth (depth);
    validateTdApiClass (object.getClass (), false);
    writeString (
      output,
      object.getClass ().getSimpleName (),
      MAX_CLASS_NAME_BYTES,
      "class name"
    );

    Field[] fields = getSerializableFields (object.getClass ());
    output.writeInt (fields.length);
    for (Field field : fields) {
      writeString (output, field.getName (), MAX_FIELD_NAME_BYTES, "field name");
      try {
        writeValue (output, field.getType (), field.get (object), depth + 1);
      } catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
        throw new IOException (
          "Failed to read field " + object.getClass ().getSimpleName () + "." +
            field.getName (),
          e
        );
      }
    }
  }

  private static void writeValue (
    DataOutput output,
    Class<?> declaredType,
    java.lang.Object value,
    int depth
  ) throws IOException {
    checkDepth (depth);
    if (value == null) {
      if (declaredType.isPrimitive ()) {
        throw new IOException ("Primitive value is null: " + declaredType.getName ());
      }
      output.writeByte (TYPE_NULL);
      return;
    }

    if (isType (declaredType, boolean.class, Boolean.class)) {
      requireValueType (value, Boolean.class, declaredType);
      output.writeByte (TYPE_BOOLEAN);
      output.writeBoolean ((Boolean) value);
    } else if (isType (declaredType, byte.class, Byte.class)) {
      requireValueType (value, Byte.class, declaredType);
      output.writeByte (TYPE_BYTE);
      output.writeByte ((Byte) value);
    } else if (isType (declaredType, short.class, Short.class)) {
      requireValueType (value, Short.class, declaredType);
      output.writeByte (TYPE_SHORT);
      output.writeShort ((Short) value);
    } else if (isType (declaredType, int.class, Integer.class)) {
      requireValueType (value, Integer.class, declaredType);
      output.writeByte (TYPE_INT);
      output.writeInt ((Integer) value);
    } else if (isType (declaredType, long.class, Long.class)) {
      requireValueType (value, Long.class, declaredType);
      output.writeByte (TYPE_LONG);
      output.writeLong ((Long) value);
    } else if (isType (declaredType, float.class, Float.class)) {
      requireValueType (value, Float.class, declaredType);
      output.writeByte (TYPE_FLOAT);
      output.writeFloat ((Float) value);
    } else if (isType (declaredType, double.class, Double.class)) {
      requireValueType (value, Double.class, declaredType);
      output.writeByte (TYPE_DOUBLE);
      output.writeDouble ((Double) value);
    } else if (declaredType == String.class) {
      requireValueType (value, String.class, declaredType);
      output.writeByte (TYPE_STRING);
      writeString (output, (String) value, MAX_STRING_BYTES, "string");
    } else if (declaredType == byte[].class) {
      requireValueType (value, byte[].class, declaredType);
      byte[] bytes = (byte[]) value;
      checkLength (bytes.length, MAX_BYTE_ARRAY_LENGTH, "byte array length");
      output.writeByte (TYPE_BYTE_ARRAY);
      output.writeInt (bytes.length);
      output.write (bytes);
    } else if (declaredType.isArray ()) {
      if (!declaredType.isInstance (value)) {
        throw incompatibleValue (declaredType, value.getClass ());
      }
      int length = Array.getLength (value);
      checkLength (length, MAX_ARRAY_LENGTH, "array length");
      output.writeByte (TYPE_ARRAY);
      output.writeInt (length);
      for (int index = 0; index < length; index++) {
        writeValue (
          output,
          declaredType.getComponentType (),
          Array.get (value, index),
          depth + 1
        );
      }
    } else if (TdApi.Object.class.isAssignableFrom (declaredType)) {
      if (!declaredType.isInstance (value) || !(value instanceof TdApi.Object)) {
        throw incompatibleValue (declaredType, value.getClass ());
      }
      output.writeByte (TYPE_OBJECT);
      writeObject (output, (TdApi.Object) value, depth);
    } else {
      throw new IOException ("Unsupported field type: " + declaredType.getName ());
    }
  }

  private static TdApi.Object readObject (
    DataInputStream input,
    Class<? extends TdApi.Object> expectedType,
    int depth
  ) throws IOException {
    checkDepth (depth);
    String className = readString (
      input,
      MAX_CLASS_NAME_BYTES,
      "class name"
    );
    Class<? extends TdApi.Object> objectType = resolveTdApiClass (className);
    if (!expectedType.isAssignableFrom (objectType)) {
      throw new IOException (
        "Object type " + objectType.getName () + " is not assignable to " +
          expectedType.getName ()
      );
    }

    int fieldCount = readLength (input, MAX_FIELD_COUNT, "field count");
    if (fieldCount > input.available () / 5) {
      throw new IOException ("Field count exceeds remaining input: " + fieldCount);
    }
    TdApi.Object object = instantiate (objectType);
    Map<String, Field> fields = mapFields (objectType);
    Set<String> seenFields = new HashSet<> ();
    for (int index = 0; index < fieldCount; index++) {
      String fieldName = readString (
        input,
        MAX_FIELD_NAME_BYTES,
        "field name"
      );
      validateFieldName (fieldName);
      if (!seenFields.add (fieldName)) {
        throw new IOException ("Duplicate field: " + fieldName);
      }

      Field field = fields.get (fieldName);
      if (field == null) {
        skipValue (input, depth + 1);
      } else {
        java.lang.Object value = readValue (input, field.getType (), depth + 1);
        try {
          field.set (object, value);
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
          throw new IOException (
            "Failed to assign field " + objectType.getSimpleName () + "." + fieldName,
            e
          );
        }
      }
    }
    return object;
  }

  private static java.lang.Object readValue (
    DataInputStream input,
    Class<?> expectedType,
    int depth
  ) throws IOException {
    checkDepth (depth);
    int tag = input.readUnsignedByte ();
    switch (tag) {
      case TYPE_NULL:
        if (expectedType.isPrimitive ()) {
          throw new IOException ("Null value for primitive field " + expectedType.getName ());
        }
        return null;
      case TYPE_BOOLEAN:
        requireExpectedType (expectedType, boolean.class, Boolean.class, "boolean");
        return input.readBoolean ();
      case TYPE_BYTE:
        requireExpectedType (expectedType, byte.class, Byte.class, "byte");
        return input.readByte ();
      case TYPE_SHORT:
        requireExpectedType (expectedType, short.class, Short.class, "short");
        return input.readShort ();
      case TYPE_INT:
        requireExpectedType (expectedType, int.class, Integer.class, "int");
        return input.readInt ();
      case TYPE_LONG:
        requireExpectedType (expectedType, long.class, Long.class, "long");
        return input.readLong ();
      case TYPE_FLOAT:
        requireExpectedType (expectedType, float.class, Float.class, "float");
        return input.readFloat ();
      case TYPE_DOUBLE:
        requireExpectedType (expectedType, double.class, Double.class, "double");
        return input.readDouble ();
      case TYPE_STRING:
        requireExactExpectedType (expectedType, String.class, "string");
        return readString (input, MAX_STRING_BYTES, "string");
      case TYPE_BYTE_ARRAY:
        requireExactExpectedType (expectedType, byte[].class, "byte array");
        return readBytes (input, MAX_BYTE_ARRAY_LENGTH, "byte array length");
      case TYPE_ARRAY:
        return readArray (input, expectedType, depth);
      case TYPE_OBJECT:
        if (!TdApi.Object.class.isAssignableFrom (expectedType)) {
          throw incompatibleTag ("object", expectedType);
        }
        return readObject (
          input,
          expectedType.asSubclass (TdApi.Object.class),
          depth
        );
      default:
        throw new IOException ("Unknown value type tag: " + tag);
    }
  }

  private static java.lang.Object readArray (
    DataInputStream input,
    Class<?> expectedType,
    int depth
  ) throws IOException {
    if (!expectedType.isArray () || expectedType == byte[].class) {
      throw incompatibleTag ("array", expectedType);
    }
    int length = readLength (input, MAX_ARRAY_LENGTH, "array length");
    if (length > input.available ()) {
      throw new IOException ("Array length exceeds remaining input: " + length);
    }

    Class<?> componentType = expectedType.getComponentType ();
    java.lang.Object array;
    try {
      array = Array.newInstance (componentType, length);
    } catch (IllegalArgumentException e) {
      throw new IOException ("Failed to create array of " + componentType.getName (), e);
    }
    for (int index = 0; index < length; index++) {
      java.lang.Object value = readValue (input, componentType, depth + 1);
      try {
        Array.set (array, index, value);
      } catch (IllegalArgumentException e) {
        throw new IOException (
          "Failed to assign array element " + index + " of " + componentType.getName (),
          e
        );
      }
    }
    return array;
  }

  private static void skipValue (DataInputStream input, int depth) throws IOException {
    checkDepth (depth);
    int tag = input.readUnsignedByte ();
    switch (tag) {
      case TYPE_NULL:
        return;
      case TYPE_BOOLEAN:
      case TYPE_BYTE:
        input.readByte ();
        return;
      case TYPE_SHORT:
        input.readShort ();
        return;
      case TYPE_INT:
      case TYPE_FLOAT:
        input.readInt ();
        return;
      case TYPE_LONG:
      case TYPE_DOUBLE:
        input.readLong ();
        return;
      case TYPE_STRING:
        readString (input, MAX_STRING_BYTES, "string");
        return;
      case TYPE_BYTE_ARRAY:
        skipBytes (input, MAX_BYTE_ARRAY_LENGTH, "byte array length");
        return;
      case TYPE_ARRAY:
        int length = readLength (input, MAX_ARRAY_LENGTH, "array length");
        if (length > input.available ()) {
          throw new IOException ("Array length exceeds remaining input: " + length);
        }
        for (int index = 0; index < length; index++) {
          skipValue (input, depth + 1);
        }
        return;
      case TYPE_OBJECT:
        skipObject (input, depth);
        return;
      default:
        throw new IOException ("Unknown value type tag: " + tag);
    }
  }

  private static void skipObject (DataInputStream input, int depth) throws IOException {
    checkDepth (depth);
    validateClassName (readString (input, MAX_CLASS_NAME_BYTES, "class name"));
    int fieldCount = readLength (input, MAX_FIELD_COUNT, "field count");
    if (fieldCount > input.available () / 5) {
      throw new IOException ("Field count exceeds remaining input: " + fieldCount);
    }

    Set<String> seenFields = new HashSet<> ();
    for (int index = 0; index < fieldCount; index++) {
      String fieldName = readString (
        input,
        MAX_FIELD_NAME_BYTES,
        "field name"
      );
      validateFieldName (fieldName);
      if (!seenFields.add (fieldName)) {
        throw new IOException ("Duplicate field: " + fieldName);
      }
      skipValue (input, depth + 1);
    }
  }

  private static Field[] getSerializableFields (Class<?> objectType) throws IOException {
    Field[] fields;
    try {
      fields = Arrays.stream (objectType.getFields ())
        .filter (field -> Modifier.isPublic (field.getModifiers ()))
        .filter (field -> !Modifier.isStatic (field.getModifiers ()))
        .sorted (Comparator.comparing (Field::getName))
        .toArray (Field[]::new);
    } catch (SecurityException e) {
      throw new IOException ("Failed to inspect " + objectType.getName (), e);
    }
    checkLength (fields.length, MAX_FIELD_COUNT, "field count");
    for (int index = 1; index < fields.length; index++) {
      if (fields[index - 1].getName ().equals (fields[index].getName ())) {
        throw new IOException ("Duplicate reflected field: " + fields[index].getName ());
      }
    }
    return fields;
  }

  private static Map<String, Field> mapFields (Class<?> objectType) throws IOException {
    Map<String, Field> fields = new HashMap<> ();
    for (Field field : getSerializableFields (objectType)) {
      fields.put (field.getName (), field);
    }
    return fields;
  }

  private static TdApi.Object instantiate (
    Class<? extends TdApi.Object> objectType
  ) throws IOException {
    try {
      Constructor<? extends TdApi.Object> constructor = objectType.getConstructor ();
      return constructor.newInstance ();
    } catch (ReflectiveOperationException | SecurityException e) {
      throw new IOException ("Failed to instantiate " + objectType.getName (), e);
    }
  }

  private static Class<? extends TdApi.Object> resolveTdApiClass (
    String simpleName
  ) throws IOException {
    validateClassName (simpleName);
    Class<?> objectType;
    try {
      objectType = Class.forName (
        TD_API_CLASS_PREFIX + simpleName,
        false,
        TdApi.class.getClassLoader ()
      );
    } catch (ClassNotFoundException | LinkageError | SecurityException e) {
      throw new IOException ("Unknown TDLib class: " + simpleName, e);
    }
    validateTdApiClass (objectType, false);
    return objectType.asSubclass (TdApi.Object.class);
  }

  private static void validateTdApiClass (Class<?> objectType, boolean allowAbstract)
    throws IOException {
    int modifiers = objectType.getModifiers ();
    if (!TdApi.Object.class.isAssignableFrom (objectType) ||
        objectType.getDeclaringClass () != TdApi.class ||
        !Modifier.isPublic (modifiers) ||
        !Modifier.isStatic (modifiers) ||
        (!allowAbstract && Modifier.isAbstract (modifiers))) {
      throw new IOException ("Not a concrete generated TDLib class: " + objectType.getName ());
    }
    String simpleName = objectType.getSimpleName ();
    validateClassName (simpleName);
    if (!objectType.getName ().equals (TD_API_CLASS_PREFIX + simpleName)) {
      throw new IOException ("Invalid TDLib class name: " + objectType.getName ());
    }
  }

  private static void validateClassName (String className) throws IOException {
    if (!isIdentifier (className, false)) {
      throw new IOException ("Invalid TDLib class name: " + className);
    }
  }

  private static void validateFieldName (String fieldName) throws IOException {
    if (!isIdentifier (fieldName, true)) {
      throw new IOException ("Invalid field name: " + fieldName);
    }
  }

  private static boolean isIdentifier (String value, boolean allowLeadingUnderscore) {
    if (value == null || value.isEmpty ()) {
      return false;
    }
    char first = value.charAt (0);
    if (!isAsciiLetter (first) && (!allowLeadingUnderscore || first != '_')) {
      return false;
    }
    for (int index = 1; index < value.length (); index++) {
      char character = value.charAt (index);
      if (!isAsciiLetter (character) && !Character.isDigit (character) && character != '_') {
        return false;
      }
    }
    return true;
  }

  private static boolean isAsciiLetter (char character) {
    return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z';
  }

  private static void writeString (
    DataOutput output,
    String value,
    int maximumLength,
    String label
  ) throws IOException {
    if (value == null) {
      throw new IOException (label + " must not be null");
    }
    if (value.length () > maximumLength) {
      throw new IOException (label + " is too long");
    }
    ByteBuffer encoded;
    try {
      encoded = StandardCharsets.UTF_8.newEncoder ()
        .onMalformedInput (CodingErrorAction.REPORT)
        .onUnmappableCharacter (CodingErrorAction.REPORT)
        .encode (CharBuffer.wrap (value));
    } catch (CharacterCodingException e) {
      throw new IOException ("Invalid UTF-8 " + label, e);
    }
    byte[] bytes = new byte[encoded.remaining ()];
    encoded.get (bytes);
    checkLength (bytes.length, maximumLength, label + " length");
    output.writeInt (bytes.length);
    output.write (bytes);
  }

  private static String readString (
    DataInputStream input,
    int maximumLength,
    String label
  ) throws IOException {
    byte[] bytes = readBytes (input, maximumLength, label + " length");
    try {
      return StandardCharsets.UTF_8.newDecoder ()
        .onMalformedInput (CodingErrorAction.REPORT)
        .onUnmappableCharacter (CodingErrorAction.REPORT)
        .decode (ByteBuffer.wrap (bytes))
        .toString ();
    } catch (CharacterCodingException e) {
      throw new IOException ("Invalid UTF-8 " + label, e);
    }
  }

  private static byte[] readBytes (
    DataInputStream input,
    int maximumLength,
    String label
  ) throws IOException {
    int length = readLength (input, maximumLength, label);
    if (length > input.available ()) {
      throw new IOException (label + " exceeds remaining input: " + length);
    }
    byte[] bytes = new byte[length];
    input.readFully (bytes);
    return bytes;
  }

  private static void skipBytes (
    DataInputStream input,
    int maximumLength,
    String label
  ) throws IOException {
    int length = readLength (input, maximumLength, label);
    if (length > input.available ()) {
      throw new IOException (label + " exceeds remaining input: " + length);
    }
    int remaining = length;
    while (remaining > 0) {
      int skipped = input.skipBytes (remaining);
      if (skipped == 0) {
        input.readByte ();
        skipped = 1;
      }
      remaining -= skipped;
    }
  }

  private static int readLength (
    DataInputStream input,
    int maximumLength,
    String label
  ) throws IOException {
    int length = input.readInt ();
    checkLength (length, maximumLength, label);
    return length;
  }

  private static void checkLength (int length, int maximumLength, String label)
    throws IOException {
    if (length < 0 || length > maximumLength) {
      throw new IOException ("Invalid " + label + ": " + length);
    }
  }

  private static void checkDepth (int depth) throws IOException {
    if (depth > MAX_DEPTH) {
      throw new IOException ("Maximum nesting depth exceeded");
    }
  }

  private static boolean isType (Class<?> type, Class<?> primitive, Class<?> boxed) {
    return type == primitive || type == boxed;
  }

  private static void requireValueType (
    java.lang.Object value,
    Class<?> expectedType,
    Class<?> declaredType
  ) throws IOException {
    if (!expectedType.isInstance (value)) {
      throw incompatibleValue (declaredType, value.getClass ());
    }
  }

  private static void requireExpectedType (
    Class<?> expectedType,
    Class<?> primitive,
    Class<?> boxed,
    String tagName
  ) throws IOException {
    if (!isType (expectedType, primitive, boxed)) {
      throw incompatibleTag (tagName, expectedType);
    }
  }

  private static void requireExactExpectedType (
    Class<?> expectedType,
    Class<?> encodedType,
    String tagName
  ) throws IOException {
    if (expectedType != encodedType) {
      throw incompatibleTag (tagName, expectedType);
    }
  }

  private static IOException incompatibleValue (Class<?> declaredType, Class<?> actualType) {
    return new IOException (
      "Value type " + actualType.getName () + " is incompatible with " +
        declaredType.getName ()
    );
  }

  private static IOException incompatibleTag (String tagName, Class<?> expectedType) {
    return new IOException (
      "Encoded " + tagName + " is incompatible with " + expectedType.getName ()
    );
  }

  private static final class LimitedOutputStream extends OutputStream {
    private final OutputStream output;
    private final int maximumLength;
    private int length;

    private LimitedOutputStream (OutputStream output, int maximumLength) {
      this.output = output;
      this.maximumLength = maximumLength;
    }

    @Override
    public void write (int value) throws IOException {
      reserve (1);
      output.write (value);
    }

    @Override
    public void write (byte[] bytes, int offset, int length) throws IOException {
      if (bytes == null) {
        throw new NullPointerException ("bytes");
      }
      if (offset < 0 || length < 0 || length > bytes.length - offset) {
        throw new IndexOutOfBoundsException ();
      }
      reserve (length);
      output.write (bytes, offset, length);
    }

    @Override
    public void flush () throws IOException {
      output.flush ();
    }

    @Override
    public void close () throws IOException {
      output.close ();
    }

    private void reserve (int additionalLength) throws IOException {
      if (additionalLength > maximumLength - length) {
        throw new IOException ("Encoded data exceeds maximum size");
      }
      length += additionalLength;
    }
  }
}
