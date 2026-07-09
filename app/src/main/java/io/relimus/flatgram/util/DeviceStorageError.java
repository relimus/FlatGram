package io.relimus.flatgram.util;

public class DeviceStorageError extends IllegalStateException {
  public DeviceStorageError () {
  }

  public DeviceStorageError (String s) {
    super(s);
  }

  public DeviceStorageError (Throwable cause) {
    super(cause);
  }
}
