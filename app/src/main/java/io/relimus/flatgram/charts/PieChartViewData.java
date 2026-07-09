package io.relimus.flatgram.charts;

import android.animation.Animator;

import io.relimus.flatgram.charts.data.ChartData;
import io.relimus.flatgram.charts.view_data.StackLinearViewData;

public class PieChartViewData extends StackLinearViewData {

    float selectionA;
    float drawingPart;
    Animator animator;

    public PieChartViewData(ChartData.Line line) {
        super(line);
    }
}
