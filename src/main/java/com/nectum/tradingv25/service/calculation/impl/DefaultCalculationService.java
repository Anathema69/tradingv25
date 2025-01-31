package com.nectum.tradingv25.service.calculation.impl;

import com.nectum.tradingv25.cache.ListCastCache;
import com.nectum.tradingv25.indicator.ta4j.Ta4jIndicatorService;
import com.nectum.tradingv25.model.entity.HistoricalData;
import com.nectum.tradingv25.model.request.ListCastCondition;
import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.ListCastResponse;
import com.nectum.tradingv25.model.response.ListCastResultData;
import com.nectum.tradingv25.model.response.OrderedResultData;
import com.nectum.tradingv25.repository.HistoricalDataRepository;
import com.nectum.tradingv25.service.calculation.CalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCalculationService implements CalculationService {

    private final HistoricalDataRepository historicalDataRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final Ta4jIndicatorService ta4jIndicatorService;
    private final ListCastCache listCastCache;

    @Override
    public List<ListCastResponse> processConditions(ListCastRequest request) {
        // Cache check
        List<ListCastResponse> cached = listCastCache.get(request);
        if (cached != null) {
            return cached;
        }

        // Not in cache => heavy compute
        List<ListCastResponse> responses = new ArrayList<>();
        for (Long idnectum : request.getIdnectums()) {
            responses.add(processIdNectum(idnectum, request));
        }

        // Save in cache
        listCastCache.put(request, responses);

        return responses;
    }

    private ListCastResponse processIdNectum(Long idnectum, ListCastRequest request) {
        // 1) Traer TODO el histórico
        List<HistoricalData> allData = historicalDataRepository
                .findAllByIdnectumOrderByFechaAsc(idnectum);

        // 2) Construir la serie con ta4j (contiene 100% de los datos, p.e. 1990..2025)
        BarSeries fullSeries = ta4jIndicatorService.buildBarSeries("idnectum_" + idnectum, allData);

        // Cache local de indicadores
        Map<String, Indicator<Num>> indicatorCache = new HashMap<>();

        // 3) Ubicar la fecha de inicio para filtrar la parte que vamos a "mostrar"
        Date startDate = parseDate(request.getStart());
        // (Si también tienes "end" en el request, parsea endDate)

        // Este array guardará los resultados que se devolverán en el JSON
        List<ListCastResultData> resultDataList = new ArrayList<>();

        // 4) Recorrer las barras y "mostrar" sólo las que estén >= start
        for (int i = 0; i < fullSeries.getBarCount(); i++) {
            Date barDate = Date.from(fullSeries.getBar(i).getEndTime().toInstant());

            // Filtrar: si la barra es anterior a startDate, no la mostramos
            if (barDate.before(startDate)) {
                continue;
            }
            // (si tienes "endDate", también filtrar barDate.after(endDate))

            // localizamos la entidad HistoricalData correspondiente (i)
            HistoricalData hd = allData.get(i);

            // Estructura para almacenar valores
            OrderedResultData orderedData = new OrderedResultData();

            // (a) Agregamos OHLCV
            orderedData.addOHLCV(
                    hd.getOpen(),
                    hd.getMaximo(),
                    hd.getMinimo(),
                    hd.getClose(),
                    hd.getVolumen() != null ? hd.getVolumen().doubleValue() : 0.0
            );

            // (b) Procesar las condiciones
            if (request.getList_conditions_entry() != null) {
                processIndicatorsInOrder(
                        request.getList_conditions_entry(),
                        orderedData,
                        fullSeries,
                        i,                // barIndex actual
                        indicatorCache
                );
            }

            // (c) Asignar la fecha en el resultado
            orderedData.setFecha(dateFormat.format(barDate));

            orderedData.finalizeOrder();
            resultDataList.add(
                    ListCastResultData.builder()
                            .data(orderedData)
                            .build()
            );
        }

        // 5) Devolver la respuesta para este idnectum
        return ListCastResponse.builder()
                .idnectum(idnectum)
                .result(resultDataList)
                .build();
    }



    /**
     * Aplica cada condición en orden, calculando indicadores y comparándolos.
     */
    private void processIndicatorsInOrder(List<ListCastCondition> conditions,
                                          OrderedResultData orderedData,
                                          BarSeries series,
                                          int currentBarIndex,
                                          Map<String, Indicator<Num>> indicatorCache) {

        for (int i = 0; i < conditions.size(); i++) {
            ListCastCondition cond = conditions.get(i);

            // =========================
            // 1) Ajustar el day_offset para el indicador principal
            // =========================
            int offsetMain = (cond.getDay_offset() != null) ? cond.getDay_offset() : 0;
            int offsetBarIndexMain = currentBarIndex + offsetMain;

            // Manejo de rango para barIndex principal
            double mainValue;
            if (offsetBarIndexMain < 0 || offsetBarIndexMain >= series.getBarCount()) {
                // Fuera de rango => 0.00 cuandp se desfasa la fecha
                mainValue = 0.0;

            } else {
                // Llamar al ta4jIndicatorService con el barIndex desplazado
                mainValue = ta4jIndicatorService.getIndicatorValue(
                        series,
                        offsetBarIndexMain,
                        cond,
                        indicatorCache,
                        false // no es el other_indicator
                );
            }

            // Nombre del indicador (para el JSON)
            String mainName = generateIndicatorName(
                    cond.getAsset_name(),
                    cond.getIndicator(),
                    cond.getPeriod(),
                    cond.getOperador(),
                    cond.getN_operador(),
                    cond.getDay_offset()
            );
            orderedData.addIndicator(mainName, mainValue);

            // =========================
            // 2) Ajustar el day_offset para el "otro" indicador
            // =========================
            double otherValue = Double.NaN;
            if (cond.getOther_indicator() != null) {
                int offsetOther = (cond.getOther_day_offset() != null) ? cond.getOther_day_offset() : 0;
                int offsetBarIndexOther = currentBarIndex + offsetOther;

                if (offsetBarIndexOther < 0 || offsetBarIndexOther >= series.getBarCount()) {
                    // Fuera de rango => 0.00 cuandp se desfasa la fecha
                    otherValue = 0.0;
                } else {
                    otherValue = ta4jIndicatorService.getIndicatorValue(
                            series,
                            offsetBarIndexOther,
                            cond,
                            indicatorCache,
                            true // es el other_indicator
                    );
                }
                // Nombre para el JSON
                String otherName = generateIndicatorName(
                        cond.getOther_asset_name(),
                        cond.getOther_indicator(),
                        cond.getOther_period(),
                        cond.getOther_operador(),
                        cond.getOther_n_operador(),
                        cond.getOther_day_offset()
                );
                orderedData.addIndicator(otherName, otherValue);
            }

            // =========================
            // 3) Hacer la comparación lógica (main vs other o vs const)
            // =========================
            boolean decision = evaluateLogic(cond, mainValue, otherValue);
            orderedData.addEntryDecision(i, decision);
        }
    }

    /**
     * Aplica el operador lógico (==, <, <=, etc.) comparando mainVal vs otherVal o la constante.
     */
    private boolean evaluateLogic(ListCastCondition cond, double mainVal, double otherVal) {
        double rightSide = (cond.getOther_indicator() != null)
                ? otherVal
                : (cond.getConstant() != null ? cond.getConstant() : 0.0);

        // Si no hay operador definido
        if (cond.getLogic_operator() == null) return false;

        // Si alguno es NaN, la comparación normalmente dará false (según lo que decidas)
        if (Double.isNaN(mainVal) || Double.isNaN(rightSide)) {
            // Podrías return false, o ignorar, depende de tu criterio
            return false;
        }

        switch (cond.getLogic_operator()) {
            case "<":  return mainVal <  rightSide;
            case "<=": return mainVal <= rightSide;
            case "==": return Math.abs(mainVal - rightSide) < 1e-12;
            case ">=": return mainVal >= rightSide;
            case ">":  return mainVal >  rightSide;
            default:   return false;
        }
    }

    private String generateIndicatorName(Integer assetName,
                                         String indicator,
                                         Integer period,
                                         String operador,
                                         Double nOperador,
                                         Integer dayOffset) {
        return String.format(
                "%d_%s_%d_%s_%d_%d",
                assetName != null ? assetName : 0,
                indicator != null ? indicator : "NULL",
                period != null ? period : 0,
                operador != null ? operador : "sum",
                (nOperador != null ? nOperador.intValue() : 0),
                (dayOffset != null ? dayOffset : 0)
        );
    }

    private Date parseDate(String dateStr) {
        try {
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing date: " + dateStr, e);
        }
    }
}
