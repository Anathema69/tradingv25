package com.nectum.tradingv25.controller;

import com.nectum.tradingv25.model.request.ConditionRequest;
import com.nectum.tradingv25.model.response.ConditionResponse;
import com.nectum.tradingv25.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ConditionsController {

    private final LLMService llmService;

    @PostMapping(
            value = "/get_conditions",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConditionResponse> getConditions(@RequestBody ConditionRequest request) {
        log.info("Recibido request para get_conditions: {}", request);
        try {
            ConditionResponse response = llmService.processCondition(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error procesando request: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}