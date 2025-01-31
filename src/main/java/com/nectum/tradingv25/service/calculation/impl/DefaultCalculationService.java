package com.nectum.tradingv25.service.calculation.impl;

import com.nectum.tradingv25.cache.ListCastCache;
import com.nectum.tradingv25.cache.SeriesCacheService;
import com.nectum.tradingv25.model.entity.HistoricalData;
import com.nectum.tradingv25.model.request.ListCastCondition;
import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.ListCastResponse;
import com.nectum.tradingv25.model.response.ListCastResultData;
import com.nectum.tradingv25.model.response.OrderedResultData;
import com.nectum.tradingv25.service.calculation.CalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCalculationService implements CalculationService {

    private final SeriesCacheService seriesCacheService;  // <--- Nuevo
    private final ListCastCache listCastCache;            // ya existente
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public List<ListCastResponse> processConditions(ListCastRequest request) {
        // Cache de la respuesta final
        List<ListCastResponse> cached = listCastCache.get(request);
        if (cached != null) {
            return cached;
        }

        // No está en cache => computar
        List<ListCastResponse> responses = new ArrayList<>();
        for (Long idnectum : request.getIdnectums()) {
            responses.add(processIdNectum(idnectum, request));
        }

        listCastCache.put(request, responses);
        return responses;
    }

    private ListCastResponse processIdNectum(Long idnectum, ListCastRequest request) {
        // 1) Conseguir la serie COMPLETA del idnectum (1990..2025)
        BarSeries fullSeries = seriesCacheService.getOrCreateSeries(idnectum);
        int barCount = fullSeries.getBarCount();
        if (barCount == 0) {
            return ListCastResponse.builder().idnectum(idnectum).result(Collections.emptyList()).build();
        }

        // 2) Filtramos la ventana para generar la salida al usuario
        Date startDate = parseDate(request.getStart());
        Date endDate = null;
        if (request.getEnd() != null) {
            endDate = parseDate(request.getEnd());
        }

        List<ListCastResultData> resultDataList = new ArrayList<>();

        // Recorremos todas las barras de la serie
        for (int barIndex = 0; barIndex < barCount; barIndex++) {
            // Convertir el endTime de la barra a Date
            Date barDate = Date.from(fullSeries.getBar(barIndex).getEndTime().toInstant());

            if (barDate.before(startDate)) {
                continue; // todavía no alcanzamos start
            }
            if (endDate != null && barDate.after(endDate)) {
                break;    // pasamos el end
            }

            // Contruir la parte OHLCV usando la BarSeries
            double open = fullSeries.getBar(barIndex).getOpenPrice().doubleValue();
            double high = fullSeries.getBar(barIndex).getHighPrice().doubleValue();
            double low  = fullSeries.getBar(barIndex).getLowPrice().doubleValue();
            double close= fullSeries.getBar(barIndex).getClosePrice().doubleValue();
            double vol  = fullSeries.getBar(barIndex).getVolume().doubleValue();

            OrderedResultData orderedData = new OrderedResultData();
            orderedData.addOHLCV(open, high, low, close, vol);

            // 3) Procesar condiciones
            if (request.getList_conditions_entry() != null) {
                processIndicatorsInOrder(request.getList_conditions_entry(), orderedData, fullSeries, barIndex, idnectum);
            }

            // 4) Fecha final
            orderedData.setFecha(dateFormat.format(barDate));
            orderedData.finalizeOrder();

            resultDataList.add(
                    ListCastResultData.builder()
                            .data(orderedData)
                            .build()
            );
        }

        return ListCastResponse.builder()
                .idnectum(idnectum)
                .result(resultDataList)
                .build();
    }

    private void processIndicatorsInOrder(List<ListCastCondition> conditions,
                                          OrderedResultData orderedData,
                                          BarSeries series,
                                          int currentBarIndex,
                                          Long idnectum) {

        for (int i = 0; i < conditions.size(); i++) {
            ListCastCondition cond = conditions.get(i);

            // day_offset para el indicador principal
            int offsetMain = (cond.getDay_offset() != null) ? cond.getDay_offset() : 0;
            int offsetBarIndexMain = currentBarIndex + offsetMain;
            double mainValue = Double.NaN;
            if (offsetBarIndexMain >= 0 && offsetBarIndexMain < series.getBarCount()) {
                // Solicitar el indicador al cache de SeriesCacheService
                // (no es "otherIndicator")
                String indicatorName = cond.getIndicator();
                Integer period = cond.getPeriod();
                boolean isOther = false;

                // 1) Obtenemos/creamos el objeto Indicator
                // 2) Pedimos su valor en barIndex = offsetBarIndexMain
                Num val = seriesCacheService.getOrCreateIndicator(
                                idnectum,
                                indicatorName,
                                period,
                                isOther
                        )
                        .getValue(offsetBarIndexMain);
                mainValue = val.doubleValue();
            }

            // Construir nombre para JSON
            String mainName = generateIndicatorName(cond.getAsset_name(), cond.getIndicator(),
                    cond.getPeriod(), cond.getOperador(),
                    cond.getN_operador(), cond.getDay_offset());
            orderedData.addIndicator(mainName, mainValue);

            // other_indicator
            double otherValue = Double.NaN;
            if (cond.getOther_indicator() != null) {
                int offsetOther = (cond.getOther_day_offset() != null) ? cond.getOther_day_offset() : 0;
                int offsetBarIndexOther = currentBarIndex + offsetOther;
                if (offsetBarIndexOther >= 0 && offsetBarIndexOther < series.getBarCount()) {
                    // Cachear "otherIndicator"
                    String otherName = cond.getOther_indicator();
                    Integer otherPeriod = cond.getOther_period();
                    boolean isOther = true;

                    Num val2 = seriesCacheService.getOrCreateIndicator(
                                    idnectum,
                                    otherName,
                                    otherPeriod,
                                    isOther
                            )
                            .getValue(offsetBarIndexOther);
                    otherValue = val2.doubleValue();
                }
                // Nombre en el JSON
                String name2 = generateIndicatorName(cond.getOther_asset_name(), cond.getOther_indicator(),
                        cond.getOther_period(), cond.getOther_operador(),
                        cond.getOther_n_operador(), cond.getOther_day_offset());
                orderedData.addIndicator(name2, otherValue);
            }

            // Evaluar la lógica
            boolean decision = evaluateLogic(cond, mainValue, otherValue);
            orderedData.addEntryDecision(i, decision);
        }
    }

    private boolean evaluateLogic(ListCastCondition cond, double mainVal, double otherVal) {
        double rightSide = (cond.getOther_indicator() != null)
                ? otherVal
                : (cond.getConstant() != null ? cond.getConstant() : 0.0);
        if (cond.getLogic_operator() == null) return false;
        if (Double.isNaN(mainVal) || Double.isNaN(rightSide)) return false;

        switch (cond.getLogic_operator()) {
            case "<":  return mainVal <  rightSide;
            case "<=": return mainVal <= rightSide;
            case "==": return Math.abs(mainVal - rightSide) < 1e-12;
            case ">=": return mainVal >= rightSide;
            case ">":  return mainVal >  rightSide;
            default:   return false;
        }
    }

    // Genera un nombre para la clave en el JSON
    private String generateIndicatorName(Integer assetName, String indicator, Integer period,
                                         String operador, Double nOperador, Integer dayOffset) {
        return String.format("%d_%s_%d_%s_%d_%d",
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
