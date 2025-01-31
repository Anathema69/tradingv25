package com.nectum.tradingv25.cache;

import com.nectum.tradingv25.indicator.ta4j.Ta4jIndicatorService;
import com.nectum.tradingv25.model.entity.HistoricalData;
import com.nectum.tradingv25.repository.HistoricalDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cachea en memoria:
 *  - BarSeries (todos los datos históricos) por cada idnectum.
 *  - Indicadores (ej. RSI(14), maxh, etc.) construidos sobre esa BarSeries.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeriesCacheService {

    private final HistoricalDataRepository historicalDataRepository;
    private final Ta4jIndicatorService ta4jIndicatorService;

    /**
     * Mapa principal: idnectum -> BarSeries con todos los datos (ej. desde 1990 a 2025).
     */
    private final Map<Long, BarSeries> barSeriesMap = new ConcurrentHashMap<>();

    /**
     * Mapa de indicadores: clave construida en base a (idnectum + nombre + periodo + etc.) -> Indicator
     * Ejemplo de clave: "7376|RSI|14"
     */
    private final Map<String, Indicator<Num>> indicatorMap = new ConcurrentHashMap<>();

    // -------------------------------------------------------------
    // 1) OBTENER O CREAR SERIES
    // -------------------------------------------------------------
    public BarSeries getOrCreateSeries(Long idnectum) {
        // Si ya existe en cache, lo devolvemos
        if (barSeriesMap.containsKey(idnectum)) {
            return barSeriesMap.get(idnectum);
        }
        // No existe => cargar TODO el histórico de Mongo y construir la serie
        log.info("Cargando todo el histórico para idnectum {} desde Mongo...", idnectum);
        List<HistoricalData> allData = historicalDataRepository.findAllByIdnectumOrderByFechaAsc(idnectum);
        BarSeries series = ta4jIndicatorService.buildBarSeries("idnectum_" + idnectum, allData);

        // Guardamos en el mapa
        barSeriesMap.put(idnectum, series);

        return series;
    }

    // -------------------------------------------------------------
    // 2) OBTENER O CREAR INDICADORES
    // -------------------------------------------------------------
    public Indicator<Num> getOrCreateIndicator(Long idnectum,
                                               String indicatorName,
                                               Integer period,
                                               // otros parámetros que necesites
                                               boolean isOtherIndicator) {
        // Generar una clave para el mapa, e.g. "7376|rsi|14|other=false"
        String key = buildIndicatorKey(idnectum, indicatorName, period, isOtherIndicator);

        // Revisar si ya está en el cache
        if (indicatorMap.containsKey(key)) {
            return indicatorMap.get(key);
        }
        // No está => construirlo
        BarSeries series = getOrCreateSeries(idnectum);

        // Podrías reusar tu IndicatorFactory o ta4jIndicatorService:
        Indicator<Num> indicator = ta4jIndicatorService.createIndicator(series, indicatorName, period);

        // Guardarlo en el mapa
        indicatorMap.put(key, indicator);

        return indicator;
    }

    // -------------------------------------------------------------
    // 3) MÉTODOS DE AYUDA Y EVICIÓN
    // -------------------------------------------------------------
    public void evictSeries(Long idnectum) {
        barSeriesMap.remove(idnectum);
        // Opcionalmente eliminar los indicadores asociados a este idnectum
        indicatorMap.keySet().removeIf(k -> k.startsWith(idnectum + "|"));
    }

    public void evictAll() {
        barSeriesMap.clear();
        indicatorMap.clear();
    }

    private String buildIndicatorKey(Long idnectum, String name, Integer period, boolean isOther) {
        // Ajusta a tu conveniencia. Separa con algún delimitador seguro, p.e. "|"
        return String.format("%d|%s|%d|%b", idnectum, name != null ? name.toLowerCase() : "null",
                period != null ? period : 0, isOther);
    }
}
