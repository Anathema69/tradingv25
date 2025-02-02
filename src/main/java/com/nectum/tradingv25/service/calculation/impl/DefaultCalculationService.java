package com.nectum.tradingv25.service.calculation.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nectum.tradingv25.indicator.ta4j.Ta4jIndicatorService;
import com.nectum.tradingv25.model.entity.HistoricalData;
import com.nectum.tradingv25.model.request.ListCastCondition;
import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.OrderedResultData;
import com.nectum.tradingv25.repository.HistoricalDataRepository;
import com.nectum.tradingv25.service.calculation.CalculationService;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultCalculationService implements CalculationService {

    private final HistoricalDataRepository historicalDataRepository;
    private final Ta4jIndicatorService ta4jIndicatorService;
    private final ObjectMapper objectMapper;

    private static final Path CACHE_DIR = Paths.get("cache");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @PostConstruct
    public void init() throws IOException {
        // Creamos el directorio de caché si no existe
        Files.createDirectories(CACHE_DIR);
    }

    /**
     * Método principal de streaming con caché en disco.
     * 1) Calcula una clave (hash MD5) a partir del request.
     * 2) Si existe en disco, lo envía directamente al cliente.
     * 3) Si no existe, llama a 'streamAndStoreToFile' para generar la respuesta
     *    y guardarla en disco simultáneamente.
     */
    @Override
    public void processConditionsStreaming(ListCastRequest request, OutputStream clientOutput) throws IOException {
        // 1) Generamos la clave de cache (por ejemplo, MD5 del JSON del request)
        String cacheKey = generateCacheKey(request);
        Path cachedFile = CACHE_DIR.resolve(cacheKey + ".jsonl");

        // 2) Revisamos si el archivo en caché ya existe
        if (Files.exists(cachedFile)) {
            log.info("Cache HIT for key: {}", cacheKey);
            // Lo leemos desde disco y lo enviamos al cliente
            streamFileToOutput(cachedFile, clientOutput);
        } else {
            log.info("Cache MISS for key: {}", cacheKey);
            // 3) Calculamos la respuesta real
            //    Al mismo tiempo la escribimos en el cliente y en un archivo.
            try (OutputStream fileOutput = Files.newOutputStream(cachedFile, StandardOpenOption.CREATE)) {
                streamAndStoreToFile(request, clientOutput, fileOutput);
            } catch (Exception e) {
                // Si hay error, borramos el archivo incompleto
                Files.deleteIfExists(cachedFile);
                throw e;
            }
        }
    }

    /**
     * Genera una clave MD5 a partir de la serialización JSON del request,
     * para usarla como nombre de archivo en caché.
     */
    private String generateCacheKey(ListCastRequest request) {
        try {
            String rawKey = objectMapper.writeValueAsString(request);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating cache key", e);
        }
    }

    /**
     * Lee un archivo en disco y lo vuelca en 'clientOutput' de forma streaming.
     */
    private void streamFileToOutput(Path file, OutputStream clientOutput) throws IOException {
        try (InputStream fis = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                clientOutput.write(buffer, 0, read);
                clientOutput.flush();
            }
        }
    }

    /**
     * Este método:
     *   - hace la paginación sobre la BD,
     *   - construye los objetos JSON,
     *   - y escribe cada trozo simultáneamente al 'clientOutput' y al 'fileOutput'.
     *
     * Así, al terminar, tendremos un archivo en caché con la misma respuesta que mandamos al cliente.
     */
    private void streamAndStoreToFile(ListCastRequest request,
                                      OutputStream clientOutput,
                                      OutputStream fileOutput) throws IOException {

        Date startDate = parseDate(request.getStart());
        // (si manejas "end" en el request, podrías parsearlo y aplicarlo también)

        for (Long idnectum : request.getIdnectums()) {
            // Cabecera JSON para cada idnectum => {"idnectum":X,"result":[
            String head = "{\"idnectum\":" + idnectum + ",\"result\":[";
            writeToBoth(head, clientOutput, fileOutput);

            boolean firstRecord = true;

            // Ejemplo de "estado global" si necesitas maxh incremental
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

                // Construir la serie con la "chunk" actual (si usas RSI, ADX, etc.)
                BarSeries series = ta4jIndicatorService.buildBarSeries(
                        "idnectum_" + idnectum + "_page_" + pageNumber,
                        chunk
                );

                // Cache local de indicadores
                Map<String, Indicator<Num>> indicatorCache = new HashMap<>();

                // Recorrer la 'chunk'
                for (int i = 0; i < chunk.size(); i++) {
                    HistoricalData hd = chunk.get(i);

                    OrderedResultData ord = new OrderedResultData();
                    ord.addOHLCV(hd.getOpen(), hd.getMaximo(), hd.getMinimo(), hd.getClose(),
                            hd.getVolumen() != null ? hd.getVolumen().doubleValue() : 0.0);

                    // Si tienes condiciones, evalúalas
                    if (request.getList_conditions_entry() != null) {
                        processIndicatorsInOrder(request.getList_conditions_entry(),
                                ord,
                                series,
                                i,
                                indicatorCache,
                                globalState // para maxh, etc.
                        );
                    }

                    // Fecha
                    ord.setFecha(sdf.format(hd.getFecha()));
                    ord.finalizeOrder();

                    // Coma antes de cada registro, salvo el primero
                    if (!firstRecord) {
                        writeToBoth(",", clientOutput, fileOutput);
                    }
                    firstRecord = false;

                    // Serializar ord a JSON
                    byte[] rowJson = objectMapper.writeValueAsBytes(ord);

                    // Escribir a ambos outputs
                    writeToBoth(rowJson, clientOutput, fileOutput);
                }

                if (page.isLast()) {
                    break;
                }
                pageNumber++;
            }

            // Cerrar array y saltar de línea => ]}
            writeToBoth("]}\n", clientOutput, fileOutput);
        }
    }

    /**
     * Escribe un string en ambos outputs (cliente y archivo).
     */
    private void writeToBoth(String data, OutputStream clientOut, OutputStream fileOut) throws IOException {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        writeToBoth(bytes, clientOut, fileOut);
    }

    /**
     * Versión para bytes[] (por si ya serializaste un objeto).
     */
    private void writeToBoth(byte[] data, OutputStream clientOut, OutputStream fileOut) throws IOException {
        clientOut.write(data);
        clientOut.flush();

        fileOut.write(data);
        fileOut.flush();
    }

    /**
     * Aplica todas las condiciones en el barIndex dado.
     * Incluye 'globalState' para maxh incremental, etc.
     */
    private void processIndicatorsInOrder(List<ListCastCondition> conditions,
                                          OrderedResultData orderedData,
                                          BarSeries series,
                                          int barIndex,
                                          Map<String, Indicator<Num>> indicatorCache,
                                          Map<String, Object> globalState) {

        for (int cIndex = 0; cIndex < conditions.size(); cIndex++) {
            ListCastCondition cond = conditions.get(cIndex);

            // Ejemplo: "maxh" => running max
            if (cond.getIndicator() != null && cond.getIndicator().toLowerCase().contains("maxh")) {
                double runningMax = (double) globalState.getOrDefault("runningMax", Double.NEGATIVE_INFINITY);
                double currentHigh = series.getBar(barIndex).getHighPrice().doubleValue();
                double newMax = Math.max(runningMax, currentHigh);
                globalState.put("runningMax", newMax);

                // Nombre "maxh"
                orderedData.addIndicator("maxh", newMax);

                // Comparación con logic_operator
                boolean decision = evaluateLogic(cond, newMax, cond.getConstant() != null ? cond.getConstant() : 0.0);
                orderedData.addEntryDecision(cIndex, decision);

            } else {
                // Lógica normal de ta4j
                int offset = (cond.getDay_offset() != null) ? cond.getDay_offset() : 0;
                int offsetIndex = barIndex + offset;
                double mainValue = Double.NaN;
                if (offsetIndex >= 0 && offsetIndex < series.getBarCount()) {
                    mainValue = ta4jIndicatorService.getIndicatorValue(series, offsetIndex, cond, indicatorCache, false);
                }

                // other_indicator
                double otherValue = Double.NaN;
                if (cond.getOther_indicator() != null) {
                    int offsetOther = (cond.getOther_day_offset() != null) ? cond.getOther_day_offset() : 0;
                    int offsetIndexOther = barIndex + offsetOther;
                    if (offsetIndexOther >= 0 && offsetIndexOther < series.getBarCount()) {
                        otherValue = ta4jIndicatorService.getIndicatorValue(series, offsetIndexOther, cond, indicatorCache, true);
                    }
                }

                // Guardar en el JSON
                orderedData.addIndicator(generateIndicatorName(cond), mainValue);

                // Evaluate logic
                boolean decision = evaluateLogic(cond, mainValue, otherValue);
                orderedData.addEntryDecision(cIndex, decision);
            }
        }
    }

    private String generateIndicatorName(ListCastCondition cond) {
        // Ejemplo sencillo
        return cond.getIndicator() + "_" + cond.getPeriod();
    }

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

    private Date parseDate(String dateStr) {
        try {
            return sdf.parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing date: " + dateStr, e);
        }
    }
}
