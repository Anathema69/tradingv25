package com.nectum.tradingv25.repository;

import com.nectum.tradingv25.model.entity.HistoricalData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Meta;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricalDataRepository extends MongoRepository<HistoricalData, String> {

    @Aggregation(pipeline = {
            "{ $match: { 'idnectum': ?0, 'fecha': { $gte: ?1 } } }",
            "{ $project: { _id: 0, idnectum: 1, fecha: 1, open: 1, maximo: 1, minimo: 1, close: 1, volumen: 1 } }",
            "{ $sort: { fecha: 1 } }"
    })
    @Meta(cursorBatchSize = 2000)
    List<HistoricalData> findByIdnectumAndFechaGreaterThanEqualOrderByFechaAsc(Long idnectum, Date startDate);

    @Query(value = "{ 'idnectum': ?0 }", exists = true)
    boolean existsByIdnectum(Long idnectum);

    @Query(value = "{ 'idnectum': ?0 }", sort = "{ 'fecha': 1 }")
    List<HistoricalData> findAllByIdnectumOrderByFechaAsc(Long idnectum);


   }