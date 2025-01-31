package com.nectum.tradingv25.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "historicosm")
public class HistoricalData {
    @Id
    private String id;

    @Indexed
    @Field("idnectum")
    private Long idnectum;

    @Field("fecha")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'")
    @Indexed
    private Date fecha;

    @Field("open")
    private Double open;

    @Field("maximo")
    private Double maximo;

    @Field("minimo")
    private Double minimo;

    @Field("close")
    private Double close;

    @Field("volumen")
    private Long volumen;

    @Field("thistoric_id")
    private Long thistoricId;

    @Field("periodo")
    private Integer periodo;
}