package com.nectum.tradingv25.service.core.impl;

import com.nectum.tradingv25.cache.ListCastCache;
import com.nectum.tradingv25.model.entity.HistoricalData;
import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.ListCastResponse;
import com.nectum.tradingv25.model.response.ListCastResultData;
import com.nectum.tradingv25.repository.HistoricalDataRepository;
import com.nectum.tradingv25.service.core.TradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingServiceImpl implements TradingService {
    private final MongoTemplate mongoTemplate;
    private final HistoricalDataRepository historicalDataRepository;
    private final ListCastCache listCastCache;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public List<ListCastResponse> processListCastConditions(ListCastRequest request) {
        List<ListCastResponse> cachedResponse = listCastCache.get(request);
        if (cachedResponse != null) {
            log.info("Retornando resultado desde caché para request: {}", request.getIdnectums());
            return cachedResponse;
        }

        log.info("Procesando nuevo request para idnectums: {}", request.getIdnectums());
        try {
            Date startDate = DATE_FORMAT.parse(request.getStart());

            List<ListCastResponse> responses = request.getIdnectums().stream()
                    .map(idnectum -> processIdnectum(idnectum, startDate))
                    .collect(Collectors.toList());

            listCastCache.put(request, responses);
            return responses;

        } catch (ParseException e) {
            log.error("Error parseando fecha: {}", request.getStart(), e);
            throw new RuntimeException("Error en formato de fecha", e);
        }
    }

    private ListCastResponse processIdnectum(Long idnectum, Date startDate) {
        Query query = new Query()
                .addCriteria(Criteria.where("idnectum").is(idnectum)
                        .and("fecha").gte(startDate));

        List<HistoricalData> historicalData = mongoTemplate.find(query, HistoricalData.class);

        List<ListCastResultData> resultData = historicalData.stream()
                .map(this::convertToResultData)
                .collect(Collectors.toList());

        return ListCastResponse.builder()
                .idnectum(idnectum)
                .result(resultData)
                .build();
    }

    private ListCastResultData convertToResultData(HistoricalData data) {


        return ListCastResultData.builder()
                .open(data.getOpen())
                .high(data.getMaximo())
                .low(data.getMinimo())
                .close(data.getClose())
                .volume(Double.valueOf(data.getVolumen()))
                .fecha(DATE_FORMAT.format(data.getFecha()))
                .build();
    }

    @Override
    public boolean validateHistoricalDataAvailability(List<Long> idnectums) {
        return idnectums.stream().allMatch(historicalDataRepository::existsByIdnectum);
    }
}