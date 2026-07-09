package io.relimus.flatgram.ui;

import android.content.Context;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.drinkless.tdlib.TdApi;
import io.relimus.flatgram.R;
import io.relimus.flatgram.navigation.HeaderView;
import io.relimus.flatgram.navigation.ViewController;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.theme.ColorId;
import io.relimus.flatgram.theme.ColorState;
import io.relimus.flatgram.theme.Theme;
import io.relimus.flatgram.tool.Screen;
import io.relimus.flatgram.widget.PopupLayout;
import io.relimus.flatgram.widget.ViewPager;

import me.vkryl.android.widget.FrameLayoutFix;

public class SetSenderController extends BottomSheetViewController<SetSenderController.Args> {
  private final Tdlib tdlib;
  private final SetSenderControllerPage setSenderControllerPage;

  public SetSenderController (@NonNull Context context, Tdlib tdlib) {
    super(context, tdlib);
    this.tdlib = tdlib;
    this.setSenderControllerPage = new SetSenderControllerPage(context, this.tdlib, this);
  }

  public void setDelegate (SetSenderControllerPage.Delegate delegate) {
    setSenderControllerPage.setDelegate(delegate);
  }

  @Override
  protected void onBeforeCreateView () {
    setSenderControllerPage.setArguments(getArguments());
    setSenderControllerPage.getValue();
  }

  @Override
  protected HeaderView onCreateHeaderView () {
    return setSenderControllerPage.getHeaderView();
  }

  @Override
  public boolean supportsBottomInset () {
    return true;
  }

  @Override
  protected void onCreateView (Context context, FrameLayoutFix contentView, ViewPager pager) {
    pager.setOffscreenPageLimit(1);
    tdlib.ui().post(this::launchOpenAnimation);
  }

  @Override
  protected void onAfterCreateView () {
    setLickViewColor(Theme.getColor(ColorId.headerLightBackground));
  }

  @Override
  public void onThemeColorsChanged (boolean areTemp, ColorState state) {
    super.onThemeColorsChanged(areTemp, state);
    setLickViewColor(Theme.getColor(ColorId.headerLightBackground));
  }

  private boolean showOverEverything = false;

  public void setShowOverEverything (boolean showOverEverything) {
    this.showOverEverything = showOverEverything;
  }

  @Override
  protected void setupPopupLayout (PopupLayout popupLayout) {
    if (showOverEverything) {
      popupLayout.setBoundController(setSenderControllerPage);
      popupLayout.setPopupHeightProvider(this);
      popupLayout.setOverlayStatusBar(true);
      popupLayout.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
      popupLayout.setTouchProvider(this);
      popupLayout.setNeedRootInsets();
      popupLayout.setActivityListener(this);
      popupLayout.setHideKeyboard();
      popupLayout.init(false);
      return;
    }
    popupLayout.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    popupLayout.setBoundController(setSenderControllerPage);
    popupLayout.setPopupHeightProvider(this);
    popupLayout.init(true);
    popupLayout.setHideKeyboard();
    popupLayout.setNeedRootInsets();
    popupLayout.setTouchProvider(this);
    popupLayout.setIgnoreHorizontal();
  }

  @Override
  protected void setDefaultListenersAndDecorators (BottomSheetBaseControllerPage controller) {
    controller.getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged (@NonNull RecyclerView recyclerView, int newState) {
      super.onScrollStateChanged(recyclerView, newState);
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        if (setSenderControllerPage.inSearchMode()) {
          if (getLickViewFactor() == 1f) {
            invalidateAllItemDecorations();
          } else {
            setSenderControllerPage.onScrollToTopRequested();
          }
        }
      }
      }
    });
    super.setDefaultListenersAndDecorators(controller);
  }

  @Override
  protected void onUpdateLickViewFactor (float factor) {
    if (headerView == null) return;

    headerView.getFilling().setShadowAlpha(factor);
  }

  @Override
  protected int getHeaderHeight () {
    return Screen.dp(56);
  }

  @Override
  protected int getPagerItemCount () {
    return 1;
  }

  @Override
  protected ViewController<?> onCreatePagerItemForPosition (Context context, int position) {
    if (position != 0) return null;
    setHeaderPosition(getContentOffset() + HeaderView.getTopOffset());
    setDefaultListenersAndDecorators(setSenderControllerPage);
    return setSenderControllerPage;
  }

  @Override
  protected int getContentOffset () {
    return (getTargetHeight() - getHeaderHeight(true)) / 2;
  }

  @Override
  protected boolean canHideByScroll () {
    return true;
  }

  @Override
  public int getId () {
    return R.id.controller_sender;
  }

  public static class Args {
    public final TdApi.Chat chat;
    public final TdApi.ChatMessageSender[] chatAvailableSenders;
    public final TdApi.MessageSender currentSender;

    public Args (TdApi.Chat chat, TdApi.ChatMessageSender[] chatAvailableSenders, TdApi.MessageSender currentSender) {
      this.chatAvailableSenders = chatAvailableSenders;
      this.currentSender = currentSender;
      this.chat = chat;
    }
  }
}
