package com.utp.control.repository;

import com.utp.control.model.entity.Record;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface RecordRepository extends R2dbcRepository<Record, Integer> {

  Mono<Record> findByIdVehicleAndDepartureDateIsNull(Integer idVehicle);

  Mono<Long> countByIdLocationAndDepartureDateIsNull(Integer idLocation);

  @Query(value = """
      INSERT INTO records (id_vehicle, id_location, id_security, entry_date, observation)
      VALUES (:idVehicle, :idLocation, :idSecurity, :entryDate, :observation)
      RETURNING id_record;
      """)
  Mono<Integer> insertRecord(@Param("idVehicle") Integer idVehicle,
                             @Param("idLocation") Integer idLocation,
                             @Param("idSecurity") Integer idSecurity,
                             @Param("entryDate") LocalDateTime entryDate,
                             @Param("observation") String observation);

  @Modifying
  @Query(value = """
      UPDATE records
      SET departure_date = :departureDate,
          observation = COALESCE(:observation, observation)
      WHERE id_record = :idRecord
        AND departure_date IS NULL;
      """)
  Mono<Long> closeRecord(@Param("idRecord") Integer idRecord,
                         @Param("departureDate") LocalDateTime departureDate,
                         @Param("observation") String observation);
}
