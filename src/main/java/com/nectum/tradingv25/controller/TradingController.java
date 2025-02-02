package com.nectum.tradingv25.controller;

import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.service.calculation.CalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TradingController {

    private final CalculationService calculationService;

    @PostMapping(value = "/list_cast_conditions", produces = "application/json")
    public StreamingResponseBody streamListCastConditions(@RequestBody ListCastRequest request) {
        // Devuelves un StreamingResponseBody
        return outputStream -> {
            calculationService.processConditionsStreamingParallel(request, outputStream);
        };
    }


}
