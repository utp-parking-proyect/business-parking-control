package com.utp.control.service;

import com.utp.control.generated.model.AccessControlIn;
import com.utp.control.generated.model.AccessControlOut;
import reactor.core.publisher.Mono;

public interface AccessControlService {

  Mono<AccessControlOut> registerEntry(Long securityUserId, AccessControlIn movement);

  Mono<AccessControlOut> registerExit(Long securityUserId, AccessControlIn movement);
}
