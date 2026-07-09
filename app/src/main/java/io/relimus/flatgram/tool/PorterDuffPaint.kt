@file:JvmName("PorterDuffPaint")

package io.relimus.flatgram.tool

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import me.vkryl.core.alphaColor
import io.relimus.flatgram.core.Lang
import io.relimus.flatgram.theme.ColorId
import io.relimus.flatgram.theme.PorterDuffColorId
import io.relimus.flatgram.theme.Theme

private fun Paint?.changePorterDuff (color: Int): Paint {
  if (this != null && this.color == color)
    return this
  val result = this ?: Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
  result.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
  result.color = color
  return result
}

@JvmName("get") @JvmOverloads fun getPorterDuffPaint (@PorterDuffColorId colorId: Int, alpha: Float = 1.0f): Paint {
  val color = if (alpha != 1.0f) {
    alphaColor(alpha, Theme.getColor(colorId))
  } else {
    Theme.getColor(colorId)
  }
  return when (colorId) {
    ColorId.background -> if (paint_background != null) { paint_background.changePorterDuff(color) } else { paint_background = paint_background.changePorterDuff(color); paint_background!! }
    ColorId.fillingPositiveContent -> if (paint_fillingPositiveContent != null) { paint_fillingPositiveContent.changePorterDuff(color) } else { paint_fillingPositiveContent = paint_fillingPositiveContent.changePorterDuff(color); paint_fillingPositiveContent!! }
    ColorId.tooltip_text -> if (paint_tooltip_text != null) { paint_tooltip_text.changePorterDuff(color) } else { paint_tooltip_text = paint_tooltip_text.changePorterDuff(color); paint_tooltip_text!! }
    ColorId.text -> if (paint_text != null) { paint_text.changePorterDuff(color) } else { paint_text = paint_text.changePorterDuff(color); paint_text!! }
    ColorId.textLight -> if (paint_textLight != null) { paint_textLight.changePorterDuff(color) } else { paint_textLight = paint_textLight.changePorterDuff(color); paint_textLight!! }
    ColorId.textSecure -> if (paint_textSecure != null) { paint_textSecure.changePorterDuff(color) } else { paint_textSecure = paint_textSecure.changePorterDuff(color); paint_textSecure!! }
    ColorId.textNeutral -> if (paint_textNeutral != null) { paint_textNeutral.changePorterDuff(color) } else { paint_textNeutral = paint_textNeutral.changePorterDuff(color); paint_textNeutral!! }
    ColorId.textNegative -> if (paint_textNegative != null) { paint_textNegative.changePorterDuff(color) } else { paint_textNegative = paint_textNegative.changePorterDuff(color); paint_textNegative!! }
    ColorId.textLink -> if (paint_textLink != null) { paint_textLink.changePorterDuff(color) } else { paint_textLink = paint_textLink.changePorterDuff(color); paint_textLink!! }
    ColorId.textSearchQueryHighlight -> if (paint_textSearchQueryHighlight != null) { paint_textSearchQueryHighlight.changePorterDuff(color) } else { paint_textSearchQueryHighlight = paint_textSearchQueryHighlight.changePorterDuff(color); paint_textSearchQueryHighlight!! }
    ColorId.background_icon -> if (paint_background_icon != null) { paint_background_icon.changePorterDuff(color) } else { paint_background_icon = paint_background_icon.changePorterDuff(color); paint_background_icon!! }
    ColorId.icon -> if (paint_icon != null) { paint_icon.changePorterDuff(color) } else { paint_icon = paint_icon.changePorterDuff(color); paint_icon!! }
    ColorId.iconActive -> if (paint_iconActive != null) { paint_iconActive.changePorterDuff(color) } else { paint_iconActive = paint_iconActive.changePorterDuff(color); paint_iconActive!! }
    ColorId.iconLight -> if (paint_iconLight != null) { paint_iconLight.changePorterDuff(color) } else { paint_iconLight = paint_iconLight.changePorterDuff(color); paint_iconLight!! }
    ColorId.iconPositive -> if (paint_iconPositive != null) { paint_iconPositive.changePorterDuff(color) } else { paint_iconPositive = paint_iconPositive.changePorterDuff(color); paint_iconPositive!! }
    ColorId.iconNegative -> if (paint_iconNegative != null) { paint_iconNegative.changePorterDuff(color) } else { paint_iconNegative = paint_iconNegative.changePorterDuff(color); paint_iconNegative!! }
    ColorId.inlineText -> if (paint_inlineText != null) { paint_inlineText.changePorterDuff(color) } else { paint_inlineText = paint_inlineText.changePorterDuff(color); paint_inlineText!! }
    ColorId.inlineIcon -> if (paint_inlineIcon != null) { paint_inlineIcon.changePorterDuff(color) } else { paint_inlineIcon = paint_inlineIcon.changePorterDuff(color); paint_inlineIcon!! }
    ColorId.circleButtonChatIcon -> if (paint_circleButtonChatIcon != null) { paint_circleButtonChatIcon.changePorterDuff(color) } else { paint_circleButtonChatIcon = paint_circleButtonChatIcon.changePorterDuff(color); paint_circleButtonChatIcon!! }
    ColorId.playerCoverIcon -> if (paint_playerCoverIcon != null) { paint_playerCoverIcon.changePorterDuff(color) } else { paint_playerCoverIcon = paint_playerCoverIcon.changePorterDuff(color); paint_playerCoverIcon!! }
    ColorId.avatar_content -> if (paint_avatar_content != null) { paint_avatar_content.changePorterDuff(color) } else { paint_avatar_content = paint_avatar_content.changePorterDuff(color); paint_avatar_content!! }
    ColorId.nameInactive -> if (paint_nameInactive != null) { paint_nameInactive.changePorterDuff(color) } else { paint_nameInactive = paint_nameInactive.changePorterDuff(color); paint_nameInactive!! }
    ColorId.nameRed -> if (paint_nameRed != null) { paint_nameRed.changePorterDuff(color) } else { paint_nameRed = paint_nameRed.changePorterDuff(color); paint_nameRed!! }
    ColorId.nameOrange -> if (paint_nameOrange != null) { paint_nameOrange.changePorterDuff(color) } else { paint_nameOrange = paint_nameOrange.changePorterDuff(color); paint_nameOrange!! }
    ColorId.nameYellow -> if (paint_nameYellow != null) { paint_nameYellow.changePorterDuff(color) } else { paint_nameYellow = paint_nameYellow.changePorterDuff(color); paint_nameYellow!! }
    ColorId.nameGreen -> if (paint_nameGreen != null) { paint_nameGreen.changePorterDuff(color) } else { paint_nameGreen = paint_nameGreen.changePorterDuff(color); paint_nameGreen!! }
    ColorId.nameCyan -> if (paint_nameCyan != null) { paint_nameCyan.changePorterDuff(color) } else { paint_nameCyan = paint_nameCyan.changePorterDuff(color); paint_nameCyan!! }
    ColorId.nameBlue -> if (paint_nameBlue != null) { paint_nameBlue.changePorterDuff(color) } else { paint_nameBlue = paint_nameBlue.changePorterDuff(color); paint_nameBlue!! }
    ColorId.nameViolet -> if (paint_nameViolet != null) { paint_nameViolet.changePorterDuff(color) } else { paint_nameViolet = paint_nameViolet.changePorterDuff(color); paint_nameViolet!! }
    ColorId.namePink -> if (paint_namePink != null) { paint_namePink.changePorterDuff(color) } else { paint_namePink = paint_namePink.changePorterDuff(color); paint_namePink!! }
    ColorId.lineInactive -> if (paint_lineInactive != null) { paint_lineInactive.changePorterDuff(color) } else { paint_lineInactive = paint_lineInactive.changePorterDuff(color); paint_lineInactive!! }
    ColorId.lineRed -> if (paint_lineRed != null) { paint_lineRed.changePorterDuff(color) } else { paint_lineRed = paint_lineRed.changePorterDuff(color); paint_lineRed!! }
    ColorId.lineOrange -> if (paint_lineOrange != null) { paint_lineOrange.changePorterDuff(color) } else { paint_lineOrange = paint_lineOrange.changePorterDuff(color); paint_lineOrange!! }
    ColorId.lineYellow -> if (paint_lineYellow != null) { paint_lineYellow.changePorterDuff(color) } else { paint_lineYellow = paint_lineYellow.changePorterDuff(color); paint_lineYellow!! }
    ColorId.lineGreen -> if (paint_lineGreen != null) { paint_lineGreen.changePorterDuff(color) } else { paint_lineGreen = paint_lineGreen.changePorterDuff(color); paint_lineGreen!! }
    ColorId.lineCyan -> if (paint_lineCyan != null) { paint_lineCyan.changePorterDuff(color) } else { paint_lineCyan = paint_lineCyan.changePorterDuff(color); paint_lineCyan!! }
    ColorId.lineBlue -> if (paint_lineBlue != null) { paint_lineBlue.changePorterDuff(color) } else { paint_lineBlue = paint_lineBlue.changePorterDuff(color); paint_lineBlue!! }
    ColorId.lineViolet -> if (paint_lineViolet != null) { paint_lineViolet.changePorterDuff(color) } else { paint_lineViolet = paint_lineViolet.changePorterDuff(color); paint_lineViolet!! }
    ColorId.linePink -> if (paint_linePink != null) { paint_linePink.changePorterDuff(color) } else { paint_linePink = paint_linePink.changePorterDuff(color); paint_linePink!! }
    ColorId.headerText -> if (paint_headerText != null) { paint_headerText.changePorterDuff(color) } else { paint_headerText = paint_headerText.changePorterDuff(color); paint_headerText!! }
    ColorId.headerIcon -> if (paint_headerIcon != null) { paint_headerIcon.changePorterDuff(color) } else { paint_headerIcon = paint_headerIcon.changePorterDuff(color); paint_headerIcon!! }
    ColorId.headerButtonIcon -> if (paint_headerButtonIcon != null) { paint_headerButtonIcon.changePorterDuff(color) } else { paint_headerButtonIcon = paint_headerButtonIcon.changePorterDuff(color); paint_headerButtonIcon!! }
    ColorId.passcodeIcon -> if (paint_passcodeIcon != null) { paint_passcodeIcon.changePorterDuff(color) } else { paint_passcodeIcon = paint_passcodeIcon.changePorterDuff(color); paint_passcodeIcon!! }
    ColorId.passcodeText -> if (paint_passcodeText != null) { paint_passcodeText.changePorterDuff(color) } else { paint_passcodeText = paint_passcodeText.changePorterDuff(color); paint_passcodeText!! }
    ColorId.ticks -> if (paint_ticks != null) { paint_ticks.changePorterDuff(color) } else { paint_ticks = paint_ticks.changePorterDuff(color); paint_ticks!! }
    ColorId.ticksRead -> if (paint_ticksRead != null) { paint_ticksRead.changePorterDuff(color) } else { paint_ticksRead = paint_ticksRead.changePorterDuff(color); paint_ticksRead!! }
    ColorId.chatListMute -> if (paint_chatListMute != null) { paint_chatListMute.changePorterDuff(color) } else { paint_chatListMute = paint_chatListMute.changePorterDuff(color); paint_chatListMute!! }
    ColorId.chatListIcon -> if (paint_chatListIcon != null) { paint_chatListIcon.changePorterDuff(color) } else { paint_chatListIcon = paint_chatListIcon.changePorterDuff(color); paint_chatListIcon!! }
    ColorId.chatListVerify -> if (paint_chatListVerify != null) { paint_chatListVerify.changePorterDuff(color) } else { paint_chatListVerify = paint_chatListVerify.changePorterDuff(color); paint_chatListVerify!! }
    ColorId.badgeText -> if (paint_badgeText != null) { paint_badgeText.changePorterDuff(color) } else { paint_badgeText = paint_badgeText.changePorterDuff(color); paint_badgeText!! }
    ColorId.badgeFailedText -> if (paint_badgeFailedText != null) { paint_badgeFailedText.changePorterDuff(color) } else { paint_badgeFailedText = paint_badgeFailedText.changePorterDuff(color); paint_badgeFailedText!! }
    ColorId.badgeMuted -> if (paint_badgeMuted != null) { paint_badgeMuted.changePorterDuff(color) } else { paint_badgeMuted = paint_badgeMuted.changePorterDuff(color); paint_badgeMuted!! }
    ColorId.badgeMutedText -> if (paint_badgeMutedText != null) { paint_badgeMutedText.changePorterDuff(color) } else { paint_badgeMutedText = paint_badgeMutedText.changePorterDuff(color); paint_badgeMutedText!! }
    ColorId.chatSendButton -> if (paint_chatSendButton != null) { paint_chatSendButton.changePorterDuff(color) } else { paint_chatSendButton = paint_chatSendButton.changePorterDuff(color); paint_chatSendButton!! }
    ColorId.messageAuthor -> if (paint_messageAuthor != null) { paint_messageAuthor.changePorterDuff(color) } else { paint_messageAuthor = paint_messageAuthor.changePorterDuff(color); paint_messageAuthor!! }
    ColorId.bubble_mediaTimeText -> if (paint_bubble_mediaTimeText != null) { paint_bubble_mediaTimeText.changePorterDuff(color) } else { paint_bubble_mediaTimeText = paint_bubble_mediaTimeText.changePorterDuff(color); paint_bubble_mediaTimeText!! }
    ColorId.bubble_mediaTimeText_noWallpaper -> if (paint_bubble_mediaTimeText_noWallpaper != null) { paint_bubble_mediaTimeText_noWallpaper.changePorterDuff(color) } else { paint_bubble_mediaTimeText_noWallpaper = paint_bubble_mediaTimeText_noWallpaper.changePorterDuff(color); paint_bubble_mediaTimeText_noWallpaper!! }
    ColorId.bubble_mediaOverlayText -> if (paint_bubble_mediaOverlayText != null) { paint_bubble_mediaOverlayText.changePorterDuff(color) } else { paint_bubble_mediaOverlayText = paint_bubble_mediaOverlayText.changePorterDuff(color); paint_bubble_mediaOverlayText!! }
    ColorId.bubbleIn_time -> if (paint_bubbleIn_time != null) { paint_bubbleIn_time.changePorterDuff(color) } else { paint_bubbleIn_time = paint_bubbleIn_time.changePorterDuff(color); paint_bubbleIn_time!! }
    ColorId.bubbleOut_time -> if (paint_bubbleOut_time != null) { paint_bubbleOut_time.changePorterDuff(color) } else { paint_bubbleOut_time = paint_bubbleOut_time.changePorterDuff(color); paint_bubbleOut_time!! }
    ColorId.bubbleOut_inlineIcon -> if (paint_bubbleOut_inlineIcon != null) { paint_bubbleOut_inlineIcon.changePorterDuff(color) } else { paint_bubbleOut_inlineIcon = paint_bubbleOut_inlineIcon.changePorterDuff(color); paint_bubbleOut_inlineIcon!! }
    ColorId.bubbleOut_waveformActive -> if (paint_bubbleOut_waveformActive != null) { paint_bubbleOut_waveformActive.changePorterDuff(color) } else { paint_bubbleOut_waveformActive = paint_bubbleOut_waveformActive.changePorterDuff(color); paint_bubbleOut_waveformActive!! }
    ColorId.bubbleOut_file -> if (paint_bubbleOut_file != null) { paint_bubbleOut_file.changePorterDuff(color) } else { paint_bubbleOut_file = paint_bubbleOut_file.changePorterDuff(color); paint_bubbleOut_file!! }
    ColorId.bubbleOut_fileContent -> if (paint_bubbleOut_fileContent != null) { paint_bubbleOut_fileContent.changePorterDuff(color) } else { paint_bubbleOut_fileContent = paint_bubbleOut_fileContent.changePorterDuff(color); paint_bubbleOut_fileContent!! }
    ColorId.bubbleOut_ticks -> if (paint_bubbleOut_ticks != null) { paint_bubbleOut_ticks.changePorterDuff(color) } else { paint_bubbleOut_ticks = paint_bubbleOut_ticks.changePorterDuff(color); paint_bubbleOut_ticks!! }
    ColorId.bubbleOut_ticksRead -> if (paint_bubbleOut_ticksRead != null) { paint_bubbleOut_ticksRead.changePorterDuff(color) } else { paint_bubbleOut_ticksRead = paint_bubbleOut_ticksRead.changePorterDuff(color); paint_bubbleOut_ticksRead!! }
    ColorId.bubbleOut_messageAuthor -> if (paint_bubbleOut_messageAuthor != null) { paint_bubbleOut_messageAuthor.changePorterDuff(color) } else { paint_bubbleOut_messageAuthor = paint_bubbleOut_messageAuthor.changePorterDuff(color); paint_bubbleOut_messageAuthor!! }
    ColorId.file -> if (paint_file != null) { paint_file.changePorterDuff(color) } else { paint_file = paint_file.changePorterDuff(color); paint_file!! }
    ColorId.fileContent -> if (paint_fileContent != null) { paint_fileContent.changePorterDuff(color) } else { paint_fileContent = paint_fileContent.changePorterDuff(color); paint_fileContent!! }
    ColorId.waveformActive -> if (paint_waveformActive != null) { paint_waveformActive.changePorterDuff(color) } else { paint_waveformActive = paint_waveformActive.changePorterDuff(color); paint_waveformActive!! }
    ColorId.waveformInactive -> if (paint_waveformInactive != null) { paint_waveformInactive.changePorterDuff(color) } else { paint_waveformInactive = paint_waveformInactive.changePorterDuff(color); paint_waveformInactive!! }
    ColorId.white -> if (paint_white != null) { paint_white.changePorterDuff(color) } else { paint_white = paint_white.changePorterDuff(color); paint_white!! }
    ColorId.blockQuoteText -> if (paint_blockQuoteText != null) { paint_blockQuoteText.changePorterDuff(color) } else { paint_blockQuoteText = paint_blockQuoteText.changePorterDuff(color); paint_blockQuoteText!! }
    ColorId.blockQuoteLine -> if (paint_blockQuoteLine != null) { paint_blockQuoteLine.changePorterDuff(color) } else { paint_blockQuoteLine = paint_blockQuoteLine.changePorterDuff(color); paint_blockQuoteLine!! }
    ColorId.bubbleIn_blockQuoteText -> if (paint_bubbleIn_blockQuoteText != null) { paint_bubbleIn_blockQuoteText.changePorterDuff(color) } else { paint_bubbleIn_blockQuoteText = paint_bubbleIn_blockQuoteText.changePorterDuff(color); paint_bubbleIn_blockQuoteText!! }
    ColorId.bubbleIn_blockQuoteLine -> if (paint_bubbleIn_blockQuoteLine != null) { paint_bubbleIn_blockQuoteLine.changePorterDuff(color) } else { paint_bubbleIn_blockQuoteLine = paint_bubbleIn_blockQuoteLine.changePorterDuff(color); paint_bubbleIn_blockQuoteLine!! }
    ColorId.bubbleOut_blockQuoteText -> if (paint_bubbleOut_blockQuoteText != null) { paint_bubbleOut_blockQuoteText.changePorterDuff(color) } else { paint_bubbleOut_blockQuoteText = paint_bubbleOut_blockQuoteText.changePorterDuff(color); paint_bubbleOut_blockQuoteText!! }
    ColorId.bubbleOut_blockQuoteLine -> if (paint_bubbleOut_blockQuoteLine != null) { paint_bubbleOut_blockQuoteLine.changePorterDuff(color) } else { paint_bubbleOut_blockQuoteLine = paint_bubbleOut_blockQuoteLine.changePorterDuff(color); paint_bubbleOut_blockQuoteLine!! }
    else -> throw IllegalArgumentException(Integer.toString(colorId))
  }
}

private var paint_background: Paint? = null
private var paint_fillingPositiveContent: Paint? = null
private var paint_tooltip_text: Paint? = null
private var paint_text: Paint? = null
private var paint_textLight: Paint? = null
private var paint_textSecure: Paint? = null
private var paint_textNeutral: Paint? = null
private var paint_textNegative: Paint? = null
private var paint_textLink: Paint? = null
private var paint_textSearchQueryHighlight: Paint? = null
private var paint_background_icon: Paint? = null
private var paint_icon: Paint? = null
private var paint_iconActive: Paint? = null
private var paint_iconLight: Paint? = null
private var paint_iconPositive: Paint? = null
private var paint_iconNegative: Paint? = null
private var paint_inlineText: Paint? = null
private var paint_inlineIcon: Paint? = null
private var paint_circleButtonChatIcon: Paint? = null
private var paint_playerCoverIcon: Paint? = null
private var paint_avatar_content: Paint? = null
private var paint_nameInactive: Paint? = null
private var paint_nameRed: Paint? = null
private var paint_nameOrange: Paint? = null
private var paint_nameYellow: Paint? = null
private var paint_nameGreen: Paint? = null
private var paint_nameCyan: Paint? = null
private var paint_nameBlue: Paint? = null
private var paint_nameViolet: Paint? = null
private var paint_namePink: Paint? = null
private var paint_lineInactive: Paint? = null
private var paint_lineRed: Paint? = null
private var paint_lineOrange: Paint? = null
private var paint_lineYellow: Paint? = null
private var paint_lineGreen: Paint? = null
private var paint_lineCyan: Paint? = null
private var paint_lineBlue: Paint? = null
private var paint_lineViolet: Paint? = null
private var paint_linePink: Paint? = null
private var paint_headerText: Paint? = null
private var paint_headerIcon: Paint? = null
private var paint_headerButtonIcon: Paint? = null
private var paint_passcodeIcon: Paint? = null
private var paint_passcodeText: Paint? = null
private var paint_ticks: Paint? = null
private var paint_ticksRead: Paint? = null
private var paint_chatListMute: Paint? = null
private var paint_chatListIcon: Paint? = null
private var paint_chatListVerify: Paint? = null
private var paint_badgeText: Paint? = null
private var paint_badgeFailedText: Paint? = null
private var paint_badgeMuted: Paint? = null
private var paint_badgeMutedText: Paint? = null
private var paint_chatSendButton: Paint? = null
private var paint_messageAuthor: Paint? = null
private var paint_bubble_mediaTimeText: Paint? = null
private var paint_bubble_mediaTimeText_noWallpaper: Paint? = null
private var paint_bubble_mediaOverlayText: Paint? = null
private var paint_bubbleIn_time: Paint? = null
private var paint_bubbleOut_time: Paint? = null
private var paint_bubbleOut_inlineIcon: Paint? = null
private var paint_bubbleOut_waveformActive: Paint? = null
private var paint_bubbleOut_file: Paint? = null
private var paint_bubbleOut_fileContent: Paint? = null
private var paint_bubbleOut_ticks: Paint? = null
private var paint_bubbleOut_ticksRead: Paint? = null
private var paint_bubbleOut_messageAuthor: Paint? = null
private var paint_file: Paint? = null
private var paint_fileContent: Paint? = null
private var paint_waveformActive: Paint? = null
private var paint_waveformInactive: Paint? = null
private var paint_white: Paint? = null
private var paint_blockQuoteText: Paint? = null
private var paint_blockQuoteLine: Paint? = null
private var paint_bubbleIn_blockQuoteText: Paint? = null
private var paint_bubbleIn_blockQuoteLine: Paint? = null
private var paint_bubbleOut_blockQuoteText: Paint? = null
private var paint_bubbleOut_blockQuoteLine: Paint? = null