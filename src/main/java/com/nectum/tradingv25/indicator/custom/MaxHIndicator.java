package com.nectum.tradingv25.indicator.custom;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class MaxHIndicator extends org.ta4j.core.indicators.CachedIndicator<Num> {

    private final Indicator<Num> highPriceIndicator;
    private final int period;

    /**
     * Constructor para el indicador MaxH.
     *
     * @param highPriceIndicator Indicador del precio alto (HighPriceIndicator).
     * @param period             Período para el cálculo del máximo.
     */
    public MaxHIndicator(Indicator<Num> highPriceIndicator, int period) {
        super(highPriceIndicator.getBarSeries());
        this.highPriceIndicator = highPriceIndicator;
        this.period = period;
    }

    @Override
    protected Num calculate(int index) {
        // Calcula el rango efectivo para el cálculo
        int startIndex = Math.max(0, index - period + 1); // Asegura que no vaya más allá del índice 0
        Num max = highPriceIndicator.getValue(startIndex);

        // Busca el valor máximo en el rango
        for (int i = startIndex + 1; i <= index; i++) {
            Num currentValue = highPriceIndicator.getValue(i);
            if (currentValue.isGreaterThan(max)) {
                max = currentValue;
            }
        }
        return max;
    }

    @Override
    public int getUnstableBars() {
        return period; // Período inicial inestable
    }
}
