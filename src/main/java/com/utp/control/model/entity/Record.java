package com.utp.control.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("records")
public class Record {

  @Id
  @Column("id_record")
  private Integer idRecord;

  @Column("id_vehicle")
  private Integer idVehicle;

  @Column("id_location")
  private Integer idLocation;

  @Column("id_security")
  private Integer idSecurity;

  @Column("entry_date")
  private LocalDateTime entryDate;

  @Column("departure_date")
  private LocalDateTime departureDate;

  @Column("observation")
  private String observation;
}
