package com.nectum.tradingv25.service.calculation;

import com.nectum.tradingv25.model.request.ListCastRequest;
import java.io.IOException;
import java.io.OutputStream;

public interface CalculationService {
    /**
     * Procesa las condiciones en modo "streaming" y va escribiendo
     * el resultado JSON al OutputStream.
     */
    void processConditionsStreaming(ListCastRequest request, OutputStream outputStream) throws IOException;
}
