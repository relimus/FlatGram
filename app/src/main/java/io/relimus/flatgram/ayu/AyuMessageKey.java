package io.relimus.flatgram.ayu;

/** Immutable account-local chat/message key used by Ayu storage. */
public final class AyuMessageKey {
  public final long chatId;
  public final long messageId;

  public AyuMessageKey (long chatId, long messageId) {
    this.chatId = chatId;
    this.messageId = messageId;
  }

  @Override
  public boolean equals (Object object) {
    if (this == object) return true;
    if (!(object instanceof AyuMessageKey)) return false;
    AyuMessageKey key = (AyuMessageKey) object;
    return chatId == key.chatId && messageId == key.messageId;
  }

  @Override
  public int hashCode () {
    int result = Long.hashCode (chatId);
    result = 31 * result + Long.hashCode (messageId);
    return result;
  }
}
