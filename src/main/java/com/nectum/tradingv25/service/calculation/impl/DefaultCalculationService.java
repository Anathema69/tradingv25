package com.nectum.tradingv25.service.calculation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nectum.tradingv25.model.entity.HistoricalData;
import com.nectum.tradingv25.model.request.ListCastCondition;
import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.OrderedResultData;
import com.nectum.tradingv25.repository.HistoricalDataRepository;
import com.nectum.tradingv25.service.calculation.CalculationService;
import com.nectum.tradingv25.indicator.ta4j.Ta4jIndicatorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultCalculationService implements CalculationService {

    private final HistoricalDataRepository historicalDataRepository;
    private final Ta4jIndicatorService ta4jIndicatorService;
    private final ObjectMapper objectMapper;

    // Ruta base donde guardarás los archivos cacheados
    private static final Path CACHE_DIR = Paths.get("cache");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(CACHE_DIR);
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void processConditionsStreaming(ListCastRequest request, OutputStream outputStream) throws IOException {


        Date startDate = parseDate(request.getStart());


        // Iteramos sobre cada idnectum
        for (Long idnectum : request.getIdnectums()) {


            // Escribe la cabecera del objeto JSON { "idnectum": X, "result": [
            String jsonHead = String.format("{\"idnectum\":%d,\"result\":[", idnectum);
            outputStream.write(jsonHead.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();


            boolean firstRecord = true;

            // ====================================================
            // Aquí guardaremos cualquier estado "acumulado" que queramos
            // mantener durante TODAS las páginas de un idnectum.
            // Ejemplo: un runningMax para maxh
            // Podríamos tener un Map para cada indicador que necesite "estado".
            // ====================================================
            Map<String, Object> globalState = new HashMap<>();
            // Por ejemplo, inicializamos un "runningMax" muy pequeño
            globalState.put("runningMax", Double.NEGATIVE_INFINITY);

            // Paginación
            int pageNumber = 0;
            int pageSize = 1000;
            while (true) {
                Page<HistoricalData> page = historicalDataRepository.findByIdnectumAndFechaGreaterThanEqual(
                        idnectum,
                        startDate,
                        PageRequest.of(pageNumber, pageSize)
                );
                List<HistoricalData> chunk = page.getContent();



                if (chunk.isEmpty()) {

                    break;
                }


                BarSeries series = ta4jIndicatorService.buildBarSeries(
                        "idnectum_" + idnectum + "_page_" + pageNumber,
                        chunk
                );

                // Cache local de indicadores (por si reúsas en la misma página)
                Map<String, Indicator<Num>> indicatorCache = new HashMap<>();

                // Recorrer cada registro en la página
                for (int i = 0; i < chunk.size(); i++) {
                    HistoricalData hd = chunk.get(i);

                    // (a) Creamos un OrderedResultData (OHLCV + indicators)
                    OrderedResultData ord = new OrderedResultData();
                    ord.addOHLCV(
                            hd.getOpen(),
                            hd.getMaximo(),
                            hd.getMinimo(),
                            hd.getClose(),
                            hd.getVolumen() != null ? hd.getVolumen().doubleValue() : 0.0
                    );
                    // Logging del "fecha"
                    log.debug("[idnectum={}] >> barIndex={}, fecha='{}'",
                            idnectum, i, hd.getFecha());

                    // (b) Procesamos indicadores
                    if (request.getList_conditions_entry() != null) {
                        processIndicatorsInOrder(
                                request.getList_conditions_entry(),
                                ord,
                                series,
                                i,
                                indicatorCache,
                                globalState // Pasamos el map para el "running max"
                        );
                    }

                    // (c) Fecha
                    ord.setFecha(sdf.format(hd.getFecha()));

                    // (d) Finalizamos
                    ord.finalizeOrder();

                    // (e) Si no es el primer record del array, añadimos una coma
                    if (!firstRecord) {
                        outputStream.write(',');
                    }
                    firstRecord = false;

                    // (f) Convertimos 'ord' a JSON y lo escribimos
                    byte[] rowJson = objectMapper.writeValueAsBytes(ord);
                    outputStream.write(rowJson);
                    outputStream.flush();
                    log.debug("[idnectum={}] >> Registrado un objeto de {} bytes",
                            idnectum, rowJson.length);
                }

                if (page.isLast()) {
                    break;
                }
                pageNumber++;
            }

            // Cerrar el array "result" y el objeto
            outputStream.write("]}".getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
            outputStream.flush();

        }

    }

    /**
     * Aplica todas las condiciones en el barIndex dado.
     * Se añade un nuevo parámetro 'globalState' que guarda estado
     * que persiste entre las páginas (ejemplo: runningMax para maxh).
     */
    private void processIndicatorsInOrder(List<ListCastCondition> conditions,
                                          OrderedResultData orderedData,
                                          BarSeries series,
                                          int barIndex,
                                          Map<String, Indicator<Num>> indicatorCache,
                                          Map<String, Object> globalState) {

        for (int cIndex = 0; cIndex < conditions.size(); cIndex++) {
            ListCastCondition cond = conditions.get(cIndex);

            // Ejemplo: si el usuario especificó "maxh: maxh" en cond.getIndicator()
            if (cond.getIndicator() != null && cond.getIndicator().toLowerCase().contains("maxh")) {
                // Haz un "running max" manual
                double runningMax = (double) globalState.getOrDefault("runningMax", Double.NEGATIVE_INFINITY);

                // Tomamos la 'high' actual de la barra
                double currentHigh = series.getBar(barIndex).getHighPrice().doubleValue();
                double newMax = Math.max(runningMax, currentHigh);

                // Actualizamos
                globalState.put("runningMax", newMax);

                // Ponemos ese valor en 'orderedData'
                // El nombre de la key en JSON puede ser "maxh" o como gustes
                String name = "maxh"; // o algo derivado de cond
                orderedData.addIndicator(name, newMax);

                // Lógica de comparación con logic_operator, etc., si corresponde
                boolean decision = evaluateLogic(cond, newMax, cond.getConstant() != null ? cond.getConstant() : 0.0);
                orderedData.addEntryDecision(cIndex, decision);
            }
            else {
                // Lógica normal de ta4j para RSI, ADX, etc.
                // 1) day_offset
                int offset = (cond.getDay_offset() != null) ? cond.getDay_offset() : 0;
                int offsetIndex = barIndex + offset;
                double mainValue = Double.NaN;
                if (offsetIndex >= 0 && offsetIndex < series.getBarCount()) {
                    mainValue = ta4jIndicatorService.getIndicatorValue(series, offsetIndex, cond, indicatorCache, false);
                }

                // Otras comparaciones "otherIndicator"
                double otherValue = Double.NaN;
                if (cond.getOther_indicator() != null) {
                    int offsetOther = (cond.getOther_day_offset() != null) ? cond.getOther_day_offset() : 0;
                    int offsetIndexOther = barIndex + offsetOther;
                    if (offsetIndexOther >= 0 && offsetIndexOther < series.getBarCount()) {
                        otherValue = ta4jIndicatorService.getIndicatorValue(series, offsetIndexOther, cond, indicatorCache, true);
                    }
                }

                // Guardamos en JSON
                String mainName = generateIndicatorName(cond);
                orderedData.addIndicator(mainName, mainValue);

                // Evaluate logic
                boolean decision = evaluateLogic(cond, mainValue, otherValue);
                orderedData.addEntryDecision(cIndex, decision);
            }
        }
    }

    // Ejemplo de evaluateLogic (igual a tu versión)
    private boolean evaluateLogic(ListCastCondition cond, double mainVal, double otherVal) {
        if (Double.isNaN(mainVal) || Double.isNaN(otherVal)) return false;
        if (cond.getLogic_operator() == null) return false;

        switch (cond.getLogic_operator()) {
            case "<":  return mainVal <  otherVal;
            case "<=": return mainVal <= otherVal;
            case "==": return Math.abs(mainVal - otherVal) < 1e-12;
            case ">=": return mainVal >= otherVal;
            case ">":  return mainVal >  otherVal;
            default:   return false;
        }
    }

    private String generateIndicatorName(ListCastCondition cond) {
        // Arma un nombre con assetName, indicator, period, etc.
        // Ejemplo simplificado:
        return cond.getIndicator() + "_" + cond.getPeriod();
    }

    private Date parseDate(String dateStr) {
        try {
            return sdf.parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing date: " + dateStr, e);
        }
    }
}
