package com.utp.control.repository;

import com.utp.control.model.entity.Location;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface LocationRepository extends R2dbcRepository<Location, Integer> {

  Flux<Location> findAllByOrderByIdLocation();

  @Query(value = """
      SELECT * FROM locations
      WHERE id_location = :idLocation
      FOR UPDATE;
      """)
  Mono<Location> findByIdForUpdate(@Param("idLocation") Integer idLocation);
}
