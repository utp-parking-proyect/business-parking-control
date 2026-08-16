package com.utp.control.util.security;

import com.utp.control.util.Constants;
import com.utp.control.util.error.ForbiddenException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class AuthenticatedUserProvider {

  private static final String CLAIM_USER_ID = "userId";

  public Mono<Long> getAuthenticatedUserId() {
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> Objects.requireNonNull(context.getAuthentication()).getPrincipal())
        .cast(Jwt.class)
        .mapNotNull(jwt -> (Number) jwt.getClaim(CLAIM_USER_ID))
        .map(Number::longValue)
        .switchIfEmpty(Mono.error(new ForbiddenException(Constants.ERROR_MISSING_USER_ID)));
  }
}
