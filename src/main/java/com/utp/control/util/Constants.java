package com.utp.control.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Constants {

  public static final String ROLE_NAME_SECURITY = "ROLE_SECURITY";

  public static final String NAME_MICROSERVICE = "business-parking-control";

  public static final String WEBSOCKET_AVAILABILITY_PATH = "/control/availability/ws";

  public static final String MESSAGE_ENTRY_REGISTERED = "Ingreso autorizado";
  public static final String MESSAGE_EXIT_REGISTERED = "Salida registrada";
  public static final String MESSAGE_VEHICLE_NOT_FOUND =
      "El vehículo no se encuentra registrado";
  public static final String MESSAGE_VEHICLE_UNASSIGNED =
      "El vehículo no está asignado a ningún usuario";
  public static final String MESSAGE_REQUEST_NOT_APPROVED =
      "El vehículo no cuenta con una autorización vigente para ingresar";
  public static final String MESSAGE_VEHICLE_ALREADY_INSIDE =
      "El vehículo ya se encuentra dentro del estacionamiento";
  public static final String MESSAGE_PARKING_FULL =
      "El estacionamiento de la sede no tiene espacios disponibles";
  public static final String MESSAGE_NO_OPEN_RECORD =
      "El vehículo no registra un ingreso pendiente de salida";
  public static final String MESSAGE_RECORD_AT_OTHER_LOCATION =
      "El vehículo ingresó por otra sede y debe registrar su salida en ella";

  public static final String ERROR_LOCATION_NOT_FOUND = "La sede indicada no existe";
  public static final String ERROR_NUMBER_PLATE_REQUIRED = "Debe indicar la placa del vehículo";
  public static final String ERROR_LOCATION_REQUIRED = "Debe indicar la sede del movimiento";
  public static final String ERROR_MISSING_USER_ID =
      "El token no contiene el identificador del usuario autenticado";
  public static final String ERROR_ENTRY_ALREADY_REGISTERED =
      "El ingreso del vehículo ya fue registrado por otra puerta";
  public static final String ERROR_EXIT_ALREADY_REGISTERED =
      "La salida del vehículo ya fue registrada por otra puerta";
  public static final String ERROR_USERS_SERVICE_UNAVAILABLE =
      "business-core-portal no se encuentra disponible";
  public static final String ERROR_REQUEST_SERVICE_UNAVAILABLE =
      "business-parking-request no se encuentra disponible";
}
