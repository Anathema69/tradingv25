package com.nectum.tradingv25.repository;

import com.nectum.tradingv25.model.entity.HistoricalData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Meta;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface HistoricalDataRepository extends MongoRepository<HistoricalData, String> {

    @Query(value = "{ 'idnectum': ?0 }", exists = true)
    boolean existsByIdnectum(Long idnectum);

    @Query(value = "{ 'idnectum': ?0 }", sort = "{ 'fecha': 1 }")
    List<HistoricalData> findAllByIdnectumOrderByFechaAsc(Long idnectum);


    @Query(value = "{ 'idnectum': ?0, 'fecha': { $gte: ?1 } }", sort = "{ 'fecha': 1 }")
    Page<HistoricalData> findByIdnectumAndFechaGreaterThanEqual(Long idnectum, Date startDate, Pageable pageable);



}