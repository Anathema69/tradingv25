package com.nectum.tradingv25.service.calculation;

import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.ListCastResponse;

import java.util.List;

public interface CalculationService {
    List<ListCastResponse> processConditions(ListCastRequest request);
}