package com.nectum.tradingv25.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIFunction {
    private String name;
    private String description;
    private Map<String, Object> parameters;

    @Data
    @Builder
    public static class Parameter {
        private String type;
        private String description;
        private List<String> enumValues;
        private Object defaultValue;
        private boolean required;
        private Map<String, Object> properties;
        private List<String> requiredProperties;
    }

    public static OpenAIFunction extractConditionFunction() {
        return OpenAIFunction.builder()
                .name("extract_condition")
                .description("Extrae la condición de trading desglosada en componentes: " +
                        "indicador, período, activo, operador, day_offset y opcionalmente otro indicador")
                .parameters(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "nombre_indicator", Map.of(
                                        "type", "string",
                                        "description", "El indicador de trading, posiblemente con una operación matemática"
                                ),
                                "opererador_indicator", Map.of(
                                        "type", "string",
                                        "enum", List.of("sum", "mult", "pow", "div", "sub", "root"),
                                        "default", "sum"
                                ),
                                "numero_operador_indicator", Map.of(
                                        "type", "number",
                                        "default", 0
                                ),
                                "period", Map.of(
                                        "type", "integer",
                                        "description", "El período del indicador"
                                ),
                                "asset", Map.of(
                                        "type", "integer",
                                        "enum", List.of(0, 1)
                                ),
                                "day_offset", Map.of(
                                        "type", "integer",
                                        "description", "El desplazamiento de días para el indicador"
                                ),
                                "operator", Map.of(
                                        "type", "string",
                                        "enum", List.of(">", "<", ">=", "<=", "==", "!=",
                                                "esta_bajando", "esta_subiendo",
                                                "esta_cruzándose_por_arriba", "esta_cruzándose_por_abajo",
                                                "se_estan_cruzando")
                                )
                        ),
                        "required", List.of("nombre_indicator", "period", "operator")
                ))
                .build();
    }
}