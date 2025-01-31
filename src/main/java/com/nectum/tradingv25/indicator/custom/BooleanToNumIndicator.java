package com.nectum.tradingv25.indicator.custom;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class BooleanToNumIndicator implements Indicator<Num> {
    private final Indicator<Boolean> booleanIndicator;

    public BooleanToNumIndicator(Indicator<Boolean> booleanIndicator) {
        this.booleanIndicator = booleanIndicator;
    }

    @Override
    public Num getValue(int index) {
        return booleanIndicator.getValue(index) ? booleanIndicator.getBarSeries().numOf(1) : booleanIndicator.getBarSeries().numOf(0);
    }

    @Override
    public org.ta4j.core.BarSeries getBarSeries() {
        return booleanIndicator.getBarSeries();
    }

    @Override
    public int getUnstableBars() {
        return booleanIndicator.getUnstableBars();
    }
}
