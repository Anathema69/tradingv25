package com.nectum.tradingv25.indicator.custom;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.RecursiveCachedIndicator;
import org.ta4j.core.num.Num;

/**
 * Calcula el valor máximo acumulado del indicador base (típicamente HighPriceIndicator)
 * desde el índice 0 hasta el índice actual.
 * maxh[i] = max(maxh[i-1], high[i]).
 */
public class MaxHIndicator extends RecursiveCachedIndicator<Num> {

    private final Indicator<Num> highIndicator;

    public MaxHIndicator(Indicator<Num> highIndicator) {
        super(highIndicator.getBarSeries());
        this.highIndicator = highIndicator;
    }

    @Override
    protected Num calculate(int index) {
        if (index == 0) {
            // El primer valor de maxh es el high del primer bar
            return highIndicator.getValue(0);
        }
        // Cálculo recursivo: max entre el valor previo y el valor actual
        Num previousMax = getValue(index - 1);
        Num currentHigh = highIndicator.getValue(index);
        return previousMax.isGreaterThan(currentHigh) ? previousMax : currentHigh;
    }

    @Override
    public int getUnstableBars() {
        // Si no consideras barras inestables, puedes devolver 0.
        // O ajustarlo según la lógica de tu sistema.
        return 0;
    }
}
