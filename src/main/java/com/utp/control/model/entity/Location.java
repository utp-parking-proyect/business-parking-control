package com.utp.control.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("locations")
public class Location {

  @Id
  @Column("id_location")
  private Integer idLocation;

  @Column("id_campus")
  private Integer idCampus;

  @Column("name_location")
  private String nameLocation;

  @Column("address")
  private String address;

  @Column("parking_capacity")
  private Integer parkingCapacity;
}
