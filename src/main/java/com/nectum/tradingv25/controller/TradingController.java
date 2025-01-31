package com.nectum.tradingv25.controller;

import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.ListCastResponse;
import com.nectum.tradingv25.service.calculation.CalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TradingController {

    private final CalculationService calculationService;

    @PostMapping("/list_cast_conditions")
    public ResponseEntity<List<ListCastResponse>> processListCastConditions(
            @RequestBody ListCastRequest request) {
        log.info("Received request for list_cast_conditions: {}", request);
        List<ListCastResponse> response = calculationService.processConditions(request);
        return ResponseEntity.ok(response);
    }
}