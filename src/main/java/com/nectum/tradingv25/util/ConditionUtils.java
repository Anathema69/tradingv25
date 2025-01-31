package com.nectum.tradingv25.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConditionUtils {

    private static final Pattern CONDITION_SEPARATOR = Pattern.compile("\\s+(y|o)\\s+", Pattern.CASE_INSENSITIVE);

    public List<String> splitConditions(String input) {
        log.debug("Separando condiciones del input: {}", input);

        // Reemplazar conectores por punto y coma
        String processedText = CONDITION_SEPARATOR.matcher(input).replaceAll(";");

        // Dividir en condiciones individuales y filtrar espacios en blanco
        List<String> conditions = Arrays.stream(processedText.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        log.debug("Condiciones separadas: {}", conditions);
        return conditions;
    }

    public boolean validateOperator(String operator) {
        return operator != null && Arrays.asList(">", "<", ">=", "<=", "==", "!=",
                "esta_bajando", "esta_subiendo",
                "esta_cruzándose_por_arriba", "esta_cruzándose_por_abajo",
                "se_estan_cruzando").contains(operator);
    }

    public boolean validateMathOperator(String operator) {
        return operator != null && Arrays.asList("sum", "mult", "pow", "div", "sub", "root")
                .contains(operator);
    }
}