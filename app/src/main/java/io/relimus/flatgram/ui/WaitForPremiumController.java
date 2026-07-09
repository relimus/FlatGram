package io.relimus.flatgram.ui;

import android.content.Context;
import android.view.View;

import org.drinkless.tdlib.TdApi;
import io.relimus.flatgram.R;
import io.relimus.flatgram.core.Lang;
import io.relimus.flatgram.telegram.Tdlib;
import io.relimus.flatgram.tool.UI;
import io.relimus.flatgram.v.CustomRecyclerView;

public class WaitForPremiumController extends RecyclerViewController<TdApi.AuthorizationStateWaitPremiumPurchase> implements View.OnClickListener {
  public WaitForPremiumController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_waitForPremium;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.login_PremiumRequiredTitle);
  }

  private boolean oneShot;

  @Override
  public void onFocus () {
    super.onFocus();
    if (!oneShot) {
      oneShot = true;
      destroyStackItemById(R.id.controller_code);
      destroyStackItemById(R.id.controller_name);
      destroyStackItemById(R.id.controller_password);
    }
  }

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    SettingsAdapter adapter = new SettingsAdapter(this);
    adapter.setItems(new ListItem[] {
      new ListItem(ListItem.TYPE_ICONIZED_EMPTY, 0, R.drawable.baseline_premium_star_96, Lang.getMarkdownString(this, R.string.login_PremiumRequired)),
      new ListItem(ListItem.TYPE_SHADOW_TOP),
      new ListItem(ListItem.TYPE_BUTTON, R.id.btn_buyPremium, 0, R.string.login_PremiumRequiredBtn),
      new ListItem(ListItem.TYPE_SHADOW_BOTTOM)
    }, false);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onClick (View v) {
    if (v.getId() == R.id.btn_buyPremium) {
      UI.openUrl("https://telegram.org/");
    }
  }
}
