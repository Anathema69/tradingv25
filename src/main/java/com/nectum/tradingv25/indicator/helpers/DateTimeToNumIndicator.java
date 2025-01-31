package com.nectum.tradingv25.indicator.helpers;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.DateTimeIndicator;
import org.ta4j.core.num.Num;

import java.time.ZonedDateTime;

public class DateTimeToNumIndicator extends CachedIndicator<Num> {

    private final DateTimeIndicator dateTimeIndicator;

    public DateTimeToNumIndicator(BarSeries series) {
        super(series);
        this.dateTimeIndicator = new DateTimeIndicator(series);
    }

    @Override
    protected Num calculate(int index) {
        ZonedDateTime dateTime = dateTimeIndicator.getValue(index);
        long epochMilli = dateTime.toInstant().toEpochMilli(); // Convertir a milisegundos desde la época
        return getBarSeries().numOf(epochMilli);
    }

    @Override
    public int getUnstableBars() {
        return dateTimeIndicator.getUnstableBars();
    }
}
