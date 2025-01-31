package com.nectum.tradingv25.indicator.helpers;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class WrapperIndicator<T> implements Indicator<Num> {
    private final Indicator<T> baseIndicator;

    public WrapperIndicator(Indicator<T> baseIndicator) {
        this.baseIndicator = baseIndicator;
    }

    @Override
    public Num getValue(int index) {
        T value = baseIndicator.getValue(index);
        if (value instanceof Boolean) {
            return baseIndicator.getBarSeries().numOf((Boolean) value ? 1 : 0);
        } else if (value instanceof Number) {
            return baseIndicator.getBarSeries().numOf(((Number) value).doubleValue());
        } else {
            throw new UnsupportedOperationException("Unsupported type: " + value.getClass());
        }
    }

    @Override
    public org.ta4j.core.BarSeries getBarSeries() {
        return baseIndicator.getBarSeries();
    }

    @Override
    public int getUnstableBars() {
        return baseIndicator.getUnstableBars();
    }
}
