package com.nectum.tradingv25.indicator.custom;

import org.ta4j.core.indicators.RecursiveCachedIndicator;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class MyRecursiveCachedIndicator extends RecursiveCachedIndicator<Num> {
    private final Indicator<Num> baseIndicator;

    public MyRecursiveCachedIndicator(Indicator<Num> baseIndicator) {
        super(baseIndicator.getBarSeries());
        this.baseIndicator = baseIndicator;
    }

    @Override
    protected Num calculate(int index) {
        // Implementa tu lógica personalizada aquí
        if (index == 0) {
            return baseIndicator.getValue(index);
        }
        return getValue(index - 1).plus(baseIndicator.getValue(index)); // Ejemplo
    }

    @Override
    public int getUnstableBars() {
        // Devuelve 0 si no tienes barras inestables
        return 0;
    }
}
