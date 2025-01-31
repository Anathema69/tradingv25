package com.nectum.tradingv25.indicator.custom;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class MyCachedIndicator extends CachedIndicator<Num> {
    private final Indicator<Num> baseIndicator;

    public MyCachedIndicator(Indicator<Num> baseIndicator) {
        super(baseIndicator.getBarSeries());
        this.baseIndicator = baseIndicator;
    }

    @Override
    protected Num calculate(int index) {
        // Implementa tu lógica personalizada aquí
        return baseIndicator.getValue(index).multipliedBy(numOf(2)); // Ejemplo
    }

    @Override
    public int getUnstableBars() {
        // Devuelve 0 si no tienes barras inestables
        return 0;
    }
}
