package com.nectum.tradingv25.service.calculation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nectum.tradingv25.model.entity.HistoricalData;
import com.nectum.tradingv25.model.request.ListCastCondition;
import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.OrderedResultData;
import com.nectum.tradingv25.repository.HistoricalDataRepository;
import com.nectum.tradingv25.service.calculation.CalculationService;
import com.nectum.tradingv25.indicator.ta4j.Ta4jIndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultCalculationService implements CalculationService {

    private final HistoricalDataRepository historicalDataRepository;
    private final Ta4jIndicatorService ta4jIndicatorService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Date parseDate(String dateStr) {
        try {
            LocalDate localDate = LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            throw new RuntimeException("Error parsing date: " + dateStr, e);
        }
    }

    private String formatDate(Date date) {
        // Convertir Date a LocalDate usando la zona horaria del sistema
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return DATE_FORMATTER.format(localDate);
    }

    // --------------------------------------------------------------------
    // Implementación secuencial (ya existente)
    // --------------------------------------------------------------------
    @Override
    public void processConditionsStreaming(ListCastRequest request, OutputStream outputStream) throws IOException {
        Date startDate = parseDate(request.getStart());

        // Iteramos sobre cada idnectum
        for (Long idnectum : request.getIdnectums()) {
            // Escribe la cabecera del objeto JSON: {"idnectum":X,"result":[
            String jsonHead = String.format("{\"idnectum\":%d,\"result\":[", idnectum);
            outputStream.write(jsonHead.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            boolean firstRecord = true;
            // Aquí se almacena el estado acumulado para el idnectum (por ejemplo, running max para "maxh")
            Map<String, Object> globalState = new HashMap<>();
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

                // Cache local de indicadores para la página
                Map<String, Indicator<Num>> indicatorCache = new HashMap<>();

                // Recorrer cada registro en la página
                for (int i = 0; i < chunk.size(); i++) {
                    HistoricalData hd = chunk.get(i);
                    // (a) Crear un OrderedResultData (OHLCV + indicadores)
                    OrderedResultData ord = new OrderedResultData();
                    ord.addOHLCV(
                            hd.getOpen(),
                            hd.getMaximo(),
                            hd.getMinimo(),
                            hd.getClose(),
                            hd.getVolumen() != null ? hd.getVolumen().doubleValue() : 0.0
                    );

                    // (b) Procesar indicadores (por ejemplo, "maxh" u otros)
                    if (request.getList_conditions_entry() != null) {
                        processIndicatorsInOrder(
                                request.getList_conditions_entry(),
                                ord,
                                series,
                                i,
                                indicatorCache,
                                globalState
                        );
                    }

                    // (c) Agregar la fecha
                    ord.setFecha(formatDate(hd.getFecha()));

                    // (d) Finalizar el objeto
                    ord.finalizeOrder();

                    // (e) Separador de registros (coma si no es el primero)
                    if (!firstRecord) {
                        outputStream.write(',');
                    }
                    firstRecord = false;

                    // (f) Convertir a JSON y escribir
                    byte[] rowJson = objectMapper.writeValueAsBytes(ord);
                    outputStream.write(rowJson);
                    outputStream.flush();
                    log.debug("[idnectum={}] >> Registrado un objeto de {} bytes", idnectum, rowJson.length);
                }

                if (page.isLast()) {
                    break;
                }
                pageNumber++;
            }

            // Cerrar el array "result" y el objeto JSON
            outputStream.write("]}".getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
            outputStream.flush();
        }
    }

    /**
     * Aplica todas las condiciones (indicadores) en el barIndex dado.
     * Se utiliza el parámetro globalState para mantener valores acumulados entre páginas.
     */
    private void processIndicatorsInOrder(List<ListCastCondition> conditions,
                                          OrderedResultData orderedData,
                                          BarSeries series,
                                          int barIndex,
                                          Map<String, Indicator<Num>> indicatorCache,
                                          Map<String, Object> globalState) {

        for (int cIndex = 0; cIndex < conditions.size(); cIndex++) {
            ListCastCondition cond = conditions.get(cIndex);

            // Procesamiento del indicador principal
            double mainValue = Double.NaN;
            if (cond.getIndicator() != null && cond.getIndicator().toLowerCase().contains("maxh")) {
                int period = (cond.getPeriod() != null) ? cond.getPeriod() : 14; // Valor por defecto
                Indicator<Num> maxhIndicator = ta4jIndicatorService.getOrCreateIndicator(series, "maxh", period, indicatorCache);
                mainValue = maxhIndicator.getValue(barIndex).doubleValue();
            } else {
                int offset = (cond.getDay_offset() != null) ? cond.getDay_offset() : 0;
                int offsetIndex = barIndex + offset;
                if (offsetIndex >= 0 && offsetIndex < series.getBarCount()) {
                    mainValue = ta4jIndicatorService.getIndicatorValue(series, offsetIndex, cond, indicatorCache, false);
                }
            }
            // Se genera el nombre usando la función unificada
            String mainName = generateIndicatorName(cond, false);
            orderedData.addIndicator(mainName, mainValue);

            // Procesamiento del otro indicador (opcional)
            double otherValue = Double.NaN;
            if (cond.getOther_indicator() != null) {
                if (cond.getOther_indicator().toLowerCase().contains("maxh")) {
                    int period = (cond.getOther_period() != null) ? cond.getOther_period() : 14;
                    Indicator<Num> maxhIndicator = ta4jIndicatorService.getOrCreateIndicator(series, "maxh", period, indicatorCache);
                    otherValue = maxhIndicator.getValue(barIndex).doubleValue();
                } else {
                    int offsetOther = (cond.getOther_day_offset() != null) ? cond.getOther_day_offset() : 0;
                    int offsetIndexOther = barIndex + offsetOther;
                    if (offsetIndexOther >= 0 && offsetIndexOther < series.getBarCount()) {
                        otherValue = ta4jIndicatorService.getIndicatorValue(series, offsetIndexOther, cond, indicatorCache, true);
                    }
                }
                String otherName = generateIndicatorName(cond, true);
                orderedData.addIndicator(otherName, otherValue);
            }

            // Evaluación de la lógica de la condición
            double comparisonValue = (cond.getOther_indicator() != null)
                    ? otherValue
                    : (cond.getConstant() != null ? cond.getConstant() : 0.0);
            boolean decision = evaluateLogic(cond, mainValue, comparisonValue);
            orderedData.addEntryDecision(cIndex, decision);
        }
    }

    /**
     * Función unificada que genera el nombre del indicador a partir del objeto cond.
     * Si isOther es true, se utilizan los campos correspondientes a other_indicator;
     * de lo contrario se utilizan los campos principales.
     */
    private String generateIndicatorName(ListCastCondition cond, boolean isOther) {
        if (isOther) {
            return String.format(
                    "%d_%s_%d_%s_%d_%d",
                    cond.getOther_asset_name() != null ? cond.getOther_asset_name() : 0,
                    cond.getOther_indicator() != null ? cond.getOther_indicator() : "",
                    cond.getOther_period() != null ? cond.getOther_period() : 0,
                    cond.getOther_operador() != null ? cond.getOther_operador() : "sum",
                    cond.getOther_n_operador() != null ? cond.getOther_n_operador().intValue() : 0,
                    cond.getOther_day_offset() != null ? cond.getOther_day_offset() : 0
            );
        } else {
            return String.format(
                    "%d_%s_%d_%s_%d_%d",
                    cond.getAsset_name() != null ? cond.getAsset_name() : 0,
                    cond.getIndicator() != null ? cond.getIndicator() : "",
                    cond.getPeriod() != null ? cond.getPeriod() : 0,
                    cond.getOperador() != null ? cond.getOperador() : "sum",
                    cond.getN_operador() != null ? cond.getN_operador().intValue() : 0,
                    cond.getDay_offset() != null ? cond.getDay_offset() : 0
            );
        }
    }

    /**
     * Evalúa la lógica de comparación entre dos valores según el operador especificado en la condición.
     */
    private boolean evaluateLogic(ListCastCondition cond, double mainVal, double otherVal) {
        if (Double.isNaN(mainVal) || Double.isNaN(otherVal)) return false;
        if (cond.getLogic_operator() == null) return false;

        switch (cond.getLogic_operator()) {
            case "<":
                return mainVal < otherVal;
            case "<=":
                return mainVal <= otherVal;
            case "==":
                return Math.abs(mainVal - otherVal) < 1e-12;
            case ">=":
                return mainVal >= otherVal;
            case ">":
                return mainVal > otherVal;
            default:
                return false;
        }
    }

    // --------------------------------------------------------------------
    // Implementación paralela nueva
    // --------------------------------------------------------------------

    /**
     * Nueva implementación: Procesa cada idnectum en paralelo utilizando un ThreadPoolExecutor.
     */
    @Override
    public void processConditionsStreamingParallel(ListCastRequest request, OutputStream outputStream) throws IOException {
        log.info("Iniciando processConditionsStreamingParallel. Idnectums: {}", request.getIdnectums());

        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores * 2);

        // Generar una tarea (Callable) por cada idnectum
        List<Long> idNectums = request.getIdnectums();
        List<Callable<IdNectumResult>> callables = new ArrayList<>();

        for (Long id : idNectums) {
            callables.add(() -> processSingleIdNectumToBytes(id, request));
        }

        long startTime = System.currentTimeMillis();
        List<Future<IdNectumResult>> futures;
        try {
            futures = executor.invokeAll(callables);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("invokeAll fue interrumpido", e);
        } finally {
            executor.shutdown(); // No se aceptan más tareas
        }
        long parallelEnd = System.currentTimeMillis();
        log.info("Todas las tareas completadas en paralelo, tiempo transcurrido: {} ms", (parallelEnd - startTime));

        // Recorrer los resultados en el mismo orden que las tareas originales
        boolean firstId = true;
        for (int i = 0; i < futures.size(); i++) {
            Long idnectum = idNectums.get(i);
            Future<IdNectumResult> future = futures.get(i);

            IdNectumResult result;
            try {
                result = future.get();
            } catch (ExecutionException e) {
                throw new RuntimeException("Error en hilo para idnectum " + idnectum, e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("El hilo principal fue interrumpido al esperar future", e);
            }

            // Escribir el JSON resultante para el idnectum
            if (!firstId) {
                // Opcional: agregar separador (por ejemplo, salto de línea)
                // outputStream.write('\n');
            }
            firstId = false;

            outputStream.write(result.getBytes());
            outputStream.flush();
        }

        long endTime = System.currentTimeMillis();
        log.info("Fin processConditionsStreamingParallel, tiempo total: {} ms", (endTime - startTime));
    }

    /**
     * Procesa un único idnectum: consulta la DB, pagina, construye el JSON y devuelve el resultado en un array de bytes.
     * Se ejecuta en un hilo aparte.
     */
    private IdNectumResult processSingleIdNectumToBytes(Long idnectum, ListCastRequest request) throws IOException {
        long t0 = System.currentTimeMillis();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            // Cabecera JSON: {"idnectum":123,"result":[
            bos.write(("{\"idnectum\":" + idnectum + ",\"result\":[").getBytes(StandardCharsets.UTF_8));

            boolean firstRecord = true;
            // Estado acumulado para este idnectum (por ejemplo, running max)
            Map<String, Object> globalState = new HashMap<>();
            globalState.put("runningMax", Double.NEGATIVE_INFINITY);

            Date startDate = parseDate(request.getStart());
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

                // Cache local de indicadores para la página
                Map<String, Indicator<Num>> indicatorCache = new HashMap<>();

                // Procesar cada registro de la página
                for (int i = 0; i < chunk.size(); i++) {
                    HistoricalData hd = chunk.get(i);
                    OrderedResultData ord = new OrderedResultData();
                    ord.addOHLCV(
                            hd.getOpen(),
                            hd.getMaximo(),
                            hd.getMinimo(),
                            hd.getClose(),
                            hd.getVolumen() != null ? hd.getVolumen().doubleValue() : 0.0
                    );

                    if (request.getList_conditions_entry() != null) {
                        processIndicatorsInOrder(
                                request.getList_conditions_entry(),
                                ord,
                                series,
                                i,
                                indicatorCache,
                                globalState
                        );
                    }

                    ord.setFecha(formatDate(hd.getFecha()));
                    ord.finalizeOrder();

                    if (!firstRecord) {
                        bos.write(',');
                    }
                    firstRecord = false;

                    byte[] rowJson = objectMapper.writeValueAsBytes(ord);
                    bos.write(rowJson);
                }

                if (page.isLast()) {
                    break;
                }
                pageNumber++;
            }

            // Cerrar el objeto JSON
            bos.write("]}".getBytes(StandardCharsets.UTF_8));
            bos.write('\n');

            bos.flush();
            long t1 = System.currentTimeMillis();

            return new IdNectumResult(idnectum, bos.toByteArray());
        }
    }

    /**
     * Clase auxiliar para devolver el resultado en bytes junto con el idnectum (útil para los logs).
     */
    private static class IdNectumResult {
        private final Long idnectum;
        private final byte[] bytes;

        public IdNectumResult(Long idnectum, byte[] bytes) {
            this.idnectum = idnectum;
            this.bytes = bytes;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public Long getIdnectum() {
            return idnectum;
        }
    }
}
