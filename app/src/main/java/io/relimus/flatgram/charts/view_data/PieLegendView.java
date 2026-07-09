package io.relimus.flatgram.charts.view_data;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.relimus.flatgram.theme.Theme;
import io.relimus.flatgram.tool.Screen;

import java.util.ArrayList;

public class PieLegendView extends LegendSignatureView {

    TextView signature;
    TextView value;

    public PieLegendView(Context context) {
        super(context);
        LinearLayout root = new LinearLayout(getContext());
        root.setPadding(Screen.dp(4), Screen.dp(2), Screen.dp(4), Screen.dp(2));
        root.addView(signature = new TextView(getContext()));
        signature.getLayoutParams().width = Screen.dp(96);
        root.addView(value = new TextView(getContext()));
        addView(root);
        value.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        setPadding(Screen.dp(12), Screen.dp(12), Screen.dp(12), Screen.dp(12));
        chevron.setVisibility(View.GONE);
        zoomEnabled = false;
    }

    public void recolor() {
        if (signature == null) {
            return;
        }
        super.recolor();
        signature.setTextColor(Theme.textAccentColor()); // key_dialogTextBlack
    }


    public void setData(String name, int value, int color) {
        signature.setText(name);
        this.value.setText(Integer.toString(value));
        this.value.setTextColor(color);
    }

    public void setSize(int n) {
    }


    public void setData(int index, long date, ArrayList<LineViewData> lines) {
    }


}
