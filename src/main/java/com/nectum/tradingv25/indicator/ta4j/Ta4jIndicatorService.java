package com.nectum.tradingv25.indicator.ta4j;

import com.nectum.tradingv25.model.entity.HistoricalData;
import com.nectum.tradingv25.model.request.ListCastCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;



@Slf4j
@Service
public class Ta4jIndicatorService {
    private final IndicatorFactory indicatorFactory = new IndicatorFactory();

    public Indicator<Num> getOrCreateIndicator(BarSeries series, String rawName, int period,
                                               Map<String, Indicator<Num>> cache) {
        String key = rawName.toLowerCase() + "_" + period;
        Indicator<Num> existing = cache.get(key);
        if (existing != null) {
            return existing;
        }
        


        Indicator<Num> created = indicatorFactory.createIndicator(series, rawName, period);
        cache.put(key, created);
        return created;
    }


    public BarSeries buildBarSeries(String seriesName, Iterable<HistoricalData> dataList) {
        // BaseBarSeries usa DecimalNum por defecto en 0.17
        BarSeries series = new BaseBarSeries(seriesName);

        for (HistoricalData hd : dataList) {
            ZonedDateTime zdt = hd.getFecha()
                    .toInstant()
                    .atZone(ZoneId.systemDefault());

            Bar bar = new BaseBar(
                    Duration.ofDays(1),
                    zdt,
                    (hd.getOpen()   != null ? hd.getOpen()   : 0.0),
                    (hd.getMaximo() != null ? hd.getMaximo() : 0.0),
                    (hd.getMinimo() != null ? hd.getMinimo() : 0.0),
                    (hd.getClose()  != null ? hd.getClose()  : 0.0),
                    (hd.getVolumen() != null ? hd.getVolumen() : 0L)
            );
            series.addBar(bar);

        }

        return series;
    }

    /**
     * Retorna el valor double del indicador en 'barIndex', aplicando operación (sum, etc.).
     */
    public double getIndicatorValue(BarSeries series,
                                    int barIndex,
                                    ListCastCondition condition,
                                    Map<String, Indicator<Num>> indicatorCache,
                                    boolean isOther) {
        String rawIndicator = isOther ? condition.getOther_indicator() : condition.getIndicator();
        Double nOperador   = isOther ? condition.getOther_n_operador() : condition.getN_operador();
        String operador    = isOther ? condition.getOther_operador()   : condition.getOperador();
        Integer period     = isOther ? condition.getOther_period()     : condition.getPeriod();

        if (rawIndicator == null) {
            return 0.0; // vs. const
        }
        if (nOperador == null) nOperador = 0.0;
        if (operador == null) operador = "sum";
        if (period == null) period = 14;

        // Usa el método getOrCreateIndicator
        Indicator<Num> taIndicator = getOrCreateIndicator(series, rawIndicator, period, indicatorCache);

        if (barIndex < 0 || barIndex >= series.getBarCount()) {
            return Double.NaN;
        }

        double baseValue = taIndicator.getValue(barIndex).doubleValue();
        return applyArithmetic(baseValue, operador, nOperador);
    }

    private double applyArithmetic(double baseValue, String operador, double nOperador) {
        switch (operador.toLowerCase()) {
            case "sum":  return baseValue + nOperador;
            case "rest": return baseValue - nOperador;
            case "mult": return baseValue * nOperador;
            case "div":  return (nOperador == 0) ? Double.NaN : baseValue / nOperador;
            case "pow":  return Math.pow(baseValue, nOperador);
            case "root": return (nOperador == 0) ? Double.NaN : Math.pow(baseValue, 1.0 / nOperador);
            default:     return baseValue;
        }
    }


}
