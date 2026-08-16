package com.utp.control.mapper;

import com.utp.control.generated.model.AccessRecord;
import com.utp.control.generated.model.LocationInformation;
import com.utp.control.model.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccessControlMapper {

  @Mapping(target = "idLocation", source = "location.idLocation")
  @Mapping(target = "nameLocation", source = "location.nameLocation")
  @Mapping(target = "address", source = "location.address")
  @Mapping(target = "campusName", source = "campusName")
  LocationInformation toLocationInformation(Location location, String campusName);

  default AccessRecord toAccessRecord(Integer idRecord, LocalDateTime entryDate,
                                      LocalDateTime departureDate) {
    if (idRecord == null) {
      return null;
    }
    return new AccessRecord()
        .idRecord(idRecord)
        .entryDate(entryDate == null ? null : entryDate.toString())
        .departureDate(departureDate == null ? null : departureDate.toString());
  }
}
