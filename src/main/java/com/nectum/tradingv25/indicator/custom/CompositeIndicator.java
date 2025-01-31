package com.nectum.tradingv25.indicator.custom;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

import java.util.Map;

public class CompositeIndicator<T extends Indicator<Num>> implements Indicator<Num> {

    private final Map<String, T> indicators;

    public CompositeIndicator(Map<String, T> indicators) {
        this.indicators = indicators;
    }

    public T getIndicator(String key) {
        return indicators.get(key);
    }

    @Override
    public Num getValue(int index) {
        throw new UnsupportedOperationException("CompositeIndicator cannot provide a single value. Use getIndicator() to retrieve specific indicators.");
    }

    @Override
    public int getUnstableBars() {
        return indicators.values().stream().mapToInt(Indicator::getUnstableBars).max().orElse(0);
    }

    @Override
    public org.ta4j.core.BarSeries getBarSeries() {
        return indicators.values().iterator().next().getBarSeries();
    }
}
