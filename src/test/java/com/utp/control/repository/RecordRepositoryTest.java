package com.utp.control.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.repository.Modifying;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecordRepositoryTest {

  @Test
  void testCloseRecord_IsModifyingSoItReturnsTheAffectedRows() throws NoSuchMethodException {
    Method closeRecord = RecordRepository.class.getMethod("closeRecord", Integer.class,
        LocalDateTime.class, String.class);

    assertNotNull(closeRecord.getAnnotation(Modifying.class),
        "closeRecord debe ser @Modifying: sin la anotación el UPDATE no devuelve las filas "
            + "afectadas y la salida se rechaza como si otra puerta la hubiera registrado");
  }
}
