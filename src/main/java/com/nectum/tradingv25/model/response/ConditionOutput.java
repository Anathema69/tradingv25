package com.nectum.tradingv25.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConditionOutput {
    private String indicator;
    private String operador;
    private Double n_operador;
    private Integer period;
    private Integer asset_name;
    private Integer day_offset;
    private String logic_operator;

    @JsonProperty("const")  // Este campo se llamará "const" en el JSON
    private Double constant;  // Pero en Java se llama "constant", porque 'const' es un nombre reservado

    private String other_indicator;
    private Double other_n_operador;
    private String other_operador;
    private Integer other_period;
    private Integer other_asset_name;
    private Integer other_day_offset;
}