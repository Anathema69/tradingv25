package com.nectum.tradingv25.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListCastRequest {
    @NotNull(message = "list_conditions_entry no puede ser null")
    private List<ListCastCondition> list_conditions_entry;

    private List<ListCastCondition> list_conditions_exit;

    @NotNull(message = "start no puede ser null")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "start debe tener formato YYYY-MM-DD")
    private String start;

    private String end;

    @NotEmpty(message = "idnectums no puede estar vacío")
    private List<Long> idnectums;

    private Double stopLoss;

    @Builder.Default
    private Boolean bajista = false;

    @Builder.Default
    private Integer periodo = 6;

    @Builder.Default
    private Integer idmercado = 8;

    @Override
    public int hashCode() {
        return Objects.hash(
                list_conditions_entry,
                list_conditions_exit,
                start,
                end,
                idnectums,
                stopLoss,
                bajista,
                periodo,
                idmercado
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ListCastRequest other = (ListCastRequest) obj;
        return Objects.equals(list_conditions_entry, other.list_conditions_entry) &&
                Objects.equals(list_conditions_exit, other.list_conditions_exit) &&
                Objects.equals(start, other.start) &&
                Objects.equals(end, other.end) &&
                Objects.equals(idnectums, other.idnectums) &&
                Objects.equals(stopLoss, other.stopLoss) &&
                Objects.equals(bajista, other.bajista) &&
                Objects.equals(periodo, other.periodo) &&
                Objects.equals(idmercado, other.idmercado);
    }
}