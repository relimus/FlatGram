package io.relimus.flatgram.ui;

import android.content.Context;
import android.view.WindowManager;

import androidx.annotation.CallSuper;

import io.relimus.flatgram.navigation.HeaderView;
import io.relimus.flatgram.navigation.ViewController;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.ui.BottomSheetViewController.BottomSheetBaseControllerPage;
import io.relimus.flatgram.widget.PopupLayout;
import io.relimus.flatgram.widget.ViewPager;

import me.vkryl.android.widget.FrameLayoutFix;

public abstract class SinglePageBottomSheetViewController<V extends ViewController<A> & BottomSheetBaseControllerPage, A> extends BottomSheetViewController<A> {

  protected final V singlePage;

  public SinglePageBottomSheetViewController (Context context, Tdlib tdlib) {
    super(context, tdlib);
    this.singlePage = onCreateSinglePage();
  }

  @Override
  protected final int getPagerItemCount () {
    return 1;
  }

  @Override
  public int getId () {
    return singlePage.getId();
  }

  @Override
  @CallSuper
  protected void onBeforeCreateView () {
    singlePage.getValue();
  }

  @Override
  public void setArguments (A args) {
    super.setArguments(args);
    singlePage.setArguments(args);
  }

  protected abstract V onCreateSinglePage ();

  @Override
  protected void onCreateView (Context context, FrameLayoutFix contentView, ViewPager pager) {
    pager.setOffscreenPageLimit(1);
  }

  @Override
  protected final ViewController<?> onCreatePagerItemForPosition (Context context, int position) {
    if (position != 0) return null;
    setHeaderPosition(getContentOffset() + HeaderView.getTopOffset());
    setDefaultListenersAndDecorators(singlePage);
    return singlePage;
  }

  @Override
  protected void setupPopupLayout (PopupLayout popupLayout) {
    popupLayout.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    popupLayout.setBoundController(singlePage);
    popupLayout.setPopupHeightProvider(this);
    popupLayout.init(true);
    popupLayout.setNeedRootInsets();
    popupLayout.setTouchProvider(this);
    popupLayout.setIgnoreHorizontal();
  }
}
