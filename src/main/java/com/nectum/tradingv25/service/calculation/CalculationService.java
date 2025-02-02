package com.nectum.tradingv25.service.calculation;

import com.nectum.tradingv25.model.request.ListCastRequest;
import java.io.IOException;
import java.io.OutputStream;

public interface CalculationService {

    /**
     * Versión existente: streaming secuencial
     */
    void processConditionsStreaming(ListCastRequest request, OutputStream outputStream) throws IOException;

    /**
     * Nueva versión: streaming en paralelo por idnectum
     */
    void processConditionsStreamingParallel(ListCastRequest request, OutputStream outputStream) throws IOException;
}
