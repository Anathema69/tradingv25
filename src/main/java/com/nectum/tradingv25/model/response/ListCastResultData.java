package com.nectum.tradingv25.model.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListCastResultData {
    private OrderedResultData data;

    @JsonValue  // Esto hará que se serialice directamente el contenido del OrderedResultData
    public Object getValue() {
        return data.getOrderedMap();
    }

    public static class ListCastResultDataBuilder {
        private OrderedResultData data = new OrderedResultData();

        public ListCastResultDataBuilder open(Double open) {
            data.addOHLCV(open,
                    data.getOrderedMap().get("high") != null ? (Double) data.getOrderedMap().get("high") : null,
                    data.getOrderedMap().get("low") != null ? (Double) data.getOrderedMap().get("low") : null,
                    data.getOrderedMap().get("close") != null ? (Double) data.getOrderedMap().get("close") : null,
                    data.getOrderedMap().get("volume") != null ? (Double) data.getOrderedMap().get("volume") : null);
            return this;
        }

        public ListCastResultDataBuilder high(Double high) {
            data.addOHLCV(
                    data.getOrderedMap().get("open") != null ? (Double) data.getOrderedMap().get("open") : null,
                    high,
                    data.getOrderedMap().get("low") != null ? (Double) data.getOrderedMap().get("low") : null,
                    data.getOrderedMap().get("close") != null ? (Double) data.getOrderedMap().get("close") : null,
                    data.getOrderedMap().get("volume") != null ? (Double) data.getOrderedMap().get("volume") : null);
            return this;
        }

        public ListCastResultDataBuilder low(Double low) {
            data.addOHLCV(
                    data.getOrderedMap().get("open") != null ? (Double) data.getOrderedMap().get("open") : null,
                    data.getOrderedMap().get("high") != null ? (Double) data.getOrderedMap().get("high") : null,
                    low,
                    data.getOrderedMap().get("close") != null ? (Double) data.getOrderedMap().get("close") : null,
                    data.getOrderedMap().get("volume") != null ? (Double) data.getOrderedMap().get("volume") : null);
            return this;
        }

        public ListCastResultDataBuilder close(Double close) {
            data.addOHLCV(
                    data.getOrderedMap().get("open") != null ? (Double) data.getOrderedMap().get("open") : null,
                    data.getOrderedMap().get("high") != null ? (Double) data.getOrderedMap().get("high") : null,
                    data.getOrderedMap().get("low") != null ? (Double) data.getOrderedMap().get("low") : null,
                    close,
                    data.getOrderedMap().get("volume") != null ? (Double) data.getOrderedMap().get("volume") : null);
            return this;
        }

        public ListCastResultDataBuilder volume(Double volume) {
            data.addOHLCV(
                    data.getOrderedMap().get("open") != null ? (Double) data.getOrderedMap().get("open") : null,
                    data.getOrderedMap().get("high") != null ? (Double) data.getOrderedMap().get("high") : null,
                    data.getOrderedMap().get("low") != null ? (Double) data.getOrderedMap().get("low") : null,
                    data.getOrderedMap().get("close") != null ? (Double) data.getOrderedMap().get("close") : null,
                    volume);
            return this;
        }

        public ListCastResultDataBuilder fecha(String fecha) {
            data.setFecha(fecha);
            data.finalizeOrder(); // Cambiado de finalize() a finalizeOrder()
            return this;
        }

        public ListCastResultData build() {
            return new ListCastResultData(data);
        }
    }
}