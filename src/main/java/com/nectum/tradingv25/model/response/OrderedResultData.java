package com.nectum.tradingv25.model.response;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
public class OrderedResultData {
    @JsonValue
    private final LinkedHashMap<String, Object> orderedMap;
    private final List<String> indicators;
    private final List<Boolean> decisions;

    public OrderedResultData() {
        this.orderedMap = new LinkedHashMap<>();
        this.indicators = new ArrayList<>();
        this.decisions = new ArrayList<>();
    }

    public void addOHLCV(Double open, Double high, Double low, Double close, Double volume) {
        orderedMap.put("open", open);
        orderedMap.put("high", high);
        orderedMap.put("low", low);
        orderedMap.put("close", close);
        orderedMap.put("volume", volume != null ? volume.longValue() : null);
    }

    public void addIndicator(String name, Double value) {
        indicators.add(name);
        orderedMap.put(name, value);
    }

    public void addEntryDecision(int index, boolean value) {
        while (decisions.size() <= index) {
            decisions.add(false);
        }
        decisions.set(index, value);
    }

    // Cambiado de finalize() a finalizeOrder()
    public void finalizeOrder() {
        // Agregar todos los indicadores en orden
        for (String indicator : indicators) {
            Object value = orderedMap.remove(indicator);
            orderedMap.put(indicator, value);
        }

        for (int i = 0; i < decisions.size(); i++) {
            orderedMap.put("entry_decicion_" + i, decisions.get(i)); // ahora guarda true/false
        }

        // Mover la fecha al final si existe
        Object fecha = orderedMap.remove("fecha");
        if (fecha != null) {
            orderedMap.put("fecha", fecha);
        }
    }

    public void setFecha(String fecha) {
        orderedMap.put("fecha", fecha);
    }
}