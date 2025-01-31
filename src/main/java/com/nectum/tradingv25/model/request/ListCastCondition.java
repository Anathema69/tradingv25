package com.nectum.tradingv25.model.request;

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
public class ListCastCondition {
    private String indicator;
    private String operador;
    private Double n_operador;
    private Integer period;
    private Integer asset_name;
    private Integer day_offset;
    private String logic_operator;

    @JsonProperty("const")
    private Double constant;

    private String other_indicator;
    private Double other_n_operador;
    private String other_operador;
    private Integer other_period;
    private Integer other_asset_name;
    private Integer other_day_offset;
}