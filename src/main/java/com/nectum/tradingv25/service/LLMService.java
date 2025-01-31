package com.nectum.tradingv25.service;

import com.nectum.tradingv25.model.request.ConditionRequest;
import com.nectum.tradingv25.model.response.ConditionResponse;

public interface LLMService {
    /**
     * Processes a condition request and returns a response
     * @param request the condition request to process
     * @return the processed condition response
     */
    ConditionResponse processCondition(ConditionRequest request);
}