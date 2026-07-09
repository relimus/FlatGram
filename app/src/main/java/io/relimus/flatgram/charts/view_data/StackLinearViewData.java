package io.relimus.flatgram.charts.view_data;

import android.graphics.Paint;

import io.relimus.flatgram.charts.BaseChartView;
import io.relimus.flatgram.charts.data.ChartData;

public class StackLinearViewData extends LineViewData {

    public StackLinearViewData(ChartData.Line line) {
        super(line);
        paint.setStyle(Paint.Style.FILL);
        if (BaseChartView.USE_LINES) {
            paint.setAntiAlias(false);
        }
    }
}
