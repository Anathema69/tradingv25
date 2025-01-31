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
        listCastCache.put(request, responses);
        return responses;
    }

    private ListCastResponse processIdNectum(Long idnectum, ListCastRequest request) {
        // 1) read from Mongo
        List<HistoricalData> historicalDataList = historicalDataRepository
                .findByIdnectumAndFechaGreaterThanEqualOrderByFechaAsc(
                        idnectum,
                        parseDate(request.getStart())
                );

        // 2) Construir la serie con DecimalNum
        BarSeries series = ta4jIndicatorService.buildBarSeries("idnectum_" + idnectum, historicalDataList);

        // Caché local de indicadores
        Map<String, Indicator<Num>> indicatorCache = new HashMap<>();
        List<ListCastResultData> resultDataList = new ArrayList<>();

        for (int i = 0; i < historicalDataList.size(); i++) {
            HistoricalData hd = historicalDataList.get(i);

            OrderedResultData orderedData = new OrderedResultData();
            // (a) OHLCV
            orderedData.addOHLCV(
                    hd.getOpen(),
                    hd.getMaximo(),
                    hd.getMinimo(),
                    hd.getClose(),
                    hd.getVolumen() != null ? hd.getVolumen().doubleValue() : 0.0
            );

            // (b) Conditions => indicators
            if (request.getList_conditions_entry() != null) {
                processIndicatorsInOrder(
                        request.getList_conditions_entry(),
                        orderedData,
                        series,
                        i,
                        indicatorCache
                );
            }

            // (c) Ajustar la fecha +1 día sólo para JSON
            Date displayedDate = adjustDateForOutput(hd.getFecha());
            orderedData.setFecha(dateFormat.format(displayedDate));

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

    /**
     * Suma 1 día para mostrar en JSON. No afecta la serie ta4j.
     */
    private Date adjustDateForOutput(Date originalDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(originalDate);
        //cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }

    private void processIndicatorsInOrder(List<ListCastCondition> conditions,
                                          OrderedResultData orderedData,
                                          BarSeries series,
                                          int barIndex,
                                          Map<String, Indicator<Num>> indicatorCache) {

        for (int i = 0; i < conditions.size(); i++) {
            ListCastCondition cond = conditions.get(i);

            double mainValue = ta4jIndicatorService.getIndicatorValue(
                    series, barIndex, cond, indicatorCache, false
            );
            String mainName = generateIndicatorName(
                    cond.getAsset_name(),
                    cond.getIndicator(),
                    cond.getPeriod(),
                    cond.getOperador(),
                    cond.getN_operador(),
                    cond.getDay_offset()
            );
            orderedData.addIndicator(mainName, mainValue);

            double otherValue = Double.NaN;
            if (cond.getOther_indicator() != null) {
                otherValue = ta4jIndicatorService.getIndicatorValue(
                        series, barIndex, cond, indicatorCache, true
                );
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

            boolean decision = evaluateLogic(cond, mainValue, otherValue);
            orderedData.addEntryDecision(i, decision);
        }
    }

    private boolean evaluateLogic(ListCastCondition cond, double mainVal, double otherVal) {
        double rightSide = (cond.getOther_indicator() != null)
                ? otherVal
                : (cond.getConstant() != null ? cond.getConstant() : 0.0);

        if (cond.getLogic_operator() == null) return false;
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
                indicator,
                period != null ? period : 0,
                operador != null ? operador : "sum",
                nOperador != null ? nOperador.intValue() : 0,
                dayOffset != null ? dayOffset : 0
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
