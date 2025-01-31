package com.nectum.tradingv25.indicator.custom;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.indicators.SMAIndicator;

public class PriceEarningsRatioIndicator extends org.ta4j.core.indicators.CachedIndicator<Num> {

    private final SMAIndicator priceSMAIndicator; // Promedio móvil del precio
    private final SMAIndicator earningsSMAIndicator; // Promedio móvil de las ganancias

    public PriceEarningsRatioIndicator(Indicator<Num> priceIndicator, Indicator<Num> earningsIndicator, int period) {
        super(priceIndicator.getBarSeries());
        this.priceSMAIndicator = new SMAIndicator(priceIndicator, period);
        this.earningsSMAIndicator = new SMAIndicator(earningsIndicator, period);
    }

    @Override
    protected Num calculate(int index) {
        Num price = priceSMAIndicator.getValue(index); // Promedio móvil del precio
        Num earnings = earningsSMAIndicator.getValue(index); // Promedio móvil de las ganancias

        if (earnings.isZero()) {
            return this.numOf(Double.POSITIVE_INFINITY); // Evita división por cero
        }

        return price.dividedBy(earnings);
    }

    @Override
    public int getUnstableBars() {
        return priceSMAIndicator.getUnstableBars(); // Hereda el comportamiento del SMA
    }
}
