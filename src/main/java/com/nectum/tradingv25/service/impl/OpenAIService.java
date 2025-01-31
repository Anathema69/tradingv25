package com.nectum.tradingv25.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nectum.tradingv25.model.request.ConditionRequest;
import com.nectum.tradingv25.model.response.ConditionOutput;
import com.nectum.tradingv25.model.response.ConditionResponse;
import com.nectum.tradingv25.service.LLMService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService implements LLMService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    // PROMPT COMBINADO CON EL ARCHIVO PYTHON Y LOS CASOS DE PRUEBA
    private static final String SYSTEM_PROMPT = """
        Eres un experto en análisis técnico que convierte condiciones financieras a un formato JSON específico.
        
        REGLAS IMPORTANTES:
        1. Devuelve SIEMPRE un JSON con la estructura exacta proporcionada.
        2. Campos obligatorios:
           - 'indicator': siempre "indicador_1".
           - 'other_indicator' (si existe): siempre "indicador_2".
           - 'operador' matemático si hay operación en el indicador.
           - 'n_operador' con el valor numérico de la operación matemática, o 0 si no hay operación.
           - 'logic_operator': ">", "<", ">=", "<=", "==", "!=", o "se_estan_cruzando".
           - 'const': valor constante si se compara con un número.
        3. Usa los siguientes operadores matemáticos:
           - '/' → "div"
           - '^' → "pow"
           - '*' → "mult"
           - '+' → "sum"
           - '-' → "sub"
        4. Si los indicadores se cruzan, utiliza "se_estan_cruzando".
        5. Asegúrate de incluir day_offset, period y asset_name (predeterminado a 0 si no se especifica).
        6. Siempre devuelve resultados en la forma JSON exacta de los ejemplos.

        EJEMPLOS DE ENTRADAS Y SALIDAS:
        Input: "EMA / 2 < 30"
        Output: {
            "indicator": "indicador_1",
            "operador": "div",
            "n_operador": 2.0,
            "period": 0,
            "asset_name": 0,
            "day_offset": 0,
            "logic_operator": "<",
            "const": 30
        }

        Input: "indicador_1 de 14 días > indicador_2 de 15 días"
        Output: {
            "indicator": "indicador_1",
            "operador": "sum",
            "n_operador": 0.0,
            "period": 14,
            "asset_name": 0,
            "day_offset": 0,
            "logic_operator": ">",
            "other_indicator": "indicador_2",
            "other_n_operador": 0.0,
            "other_operador": "sum",
            "other_period": 15,
            "other_asset_name": 0,
            "other_day_offset": 0
        }

        Input: "indicador_1 > indicador_2 de ayer"
        Output: {
            "indicator": "indicador_1",
            "operador": "sum",
            "n_operador": 0.0,
            "period": 0,
            "asset_name": 0,
            "day_offset": 0,
            "logic_operator": ">",
            "other_indicator": "indicador_2",
            "other_n_operador": 0.0,
            "other_operador": "sum",
            "other_period": 0,
            "other_asset_name": 0,
            "other_day_offset": -1
        }
        
        Input: "indicador_1 > 50"
        Output: {
            "indicator": "indicador_1",
            "operador": "sum",
            "n_operador": 0.0,
            "period": 0,
            "asset_name": 0,
            "day_offset": 0,
            "logic_operator": ">",
            "const": 50
        }
        
        Procesa la entrada cuidadosamente y devuélvela en la estructura exacta JSON anterior.
    """;

    @Override
    @Cacheable(value = "conditionsCache", key = "#request.input")
    public ConditionResponse processCondition(ConditionRequest request) {
        try {
            log.debug("Procesando condición: {}", request.getInput());

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-4o-mini")
                    .messages(Arrays.asList(
                            new ChatMessage("system", SYSTEM_PROMPT),
                            new ChatMessage("user", "Analiza y convierte a JSON: " + request.getInput())
                    ))
                    .temperature(0.0) // Mantener temperatura baja para respuestas consistentes
                    .maxTokens(750)  // Tokens suficientes para respuestas completas
                    .build();

            var completion = openAiService.createChatCompletion(completionRequest);
            String jsonResponse = completion.getChoices().get(0).getMessage().getContent();
            log.debug("Respuesta de OpenAI: {}", jsonResponse);

            // VALIDACIÓN DE LA RESPUESTA
            if (!jsonResponse.startsWith("{")) {
                throw new RuntimeException("Respuesta inválida del modelo: " + jsonResponse);
            }

            // DESERIALIZACIÓN Y ENVOLVER EN FORMATO DE RESPUESTA
            Map<String, List<ConditionOutput>> wrappedResponse = new HashMap<>();
            ConditionOutput output = objectMapper.readValue(jsonResponse, ConditionOutput.class);
            wrappedResponse.put("output", Collections.singletonList(output));

            return objectMapper.convertValue(wrappedResponse, ConditionResponse.class);

        } catch (Exception e) {
            log.error("Error procesando condición: {}", e.getMessage());
            throw new RuntimeException("Error procesando condición: " + e.getMessage());
        }
    }
}
