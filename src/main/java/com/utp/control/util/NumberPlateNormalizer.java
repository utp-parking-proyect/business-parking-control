package com.utp.control.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class NumberPlateNormalizer {

  public static String normalize(String numberPlate) {
    if (numberPlate == null || numberPlate.isBlank()) {
      throw new IllegalArgumentException(Constants.ERROR_NUMBER_PLATE_REQUIRED);
    }
    return numberPlate.trim().toUpperCase();
  }
}
