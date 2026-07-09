@file:JvmName("FallbackColorId")

package io.relimus.flatgram.theme

@JvmName("get") fun getFallbackColorId (@ColorId colorId: Int): Int = when (colorId) {
  ColorId.blockQuoteText -> ColorId.text
  ColorId.blockQuoteLine -> ColorId.text
  ColorId.bubbleIn_blockQuoteText -> ColorId.bubbleIn_time
  ColorId.bubbleIn_blockQuoteLine -> ColorId.bubbleIn_time
  ColorId.bubbleOut_blockQuoteText -> ColorId.bubbleOut_time
  ColorId.bubbleOut_blockQuoteLine -> ColorId.bubbleOut_time
  else -> ColorId.NONE
} 
