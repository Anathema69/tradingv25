package com.nectum.tradingv25.service.core;

import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.ListCastResponse;
import java.util.List;

public interface TradingService {
    List<ListCastResponse> processListCastConditions(ListCastRequest request);  //Devolver una lista
    boolean validateHistoricalDataAvailability(List<Long> idnectums);
}