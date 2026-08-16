package com.utp.control.config;

import com.utp.control.util.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

  private static final String ENTRY_PATH = "/control/entry";
  private static final String EXIT_PATH = "/control/exit";
  private static final String AVAILABILITY_PATH = "/control/availability/**";

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(auth -> auth
            .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .pathMatchers(HttpMethod.POST, ENTRY_PATH).hasAuthority(Constants.ROLE_NAME_SECURITY)
            .pathMatchers(HttpMethod.POST, EXIT_PATH).hasAuthority(Constants.ROLE_NAME_SECURITY)
            .pathMatchers(HttpMethod.GET, AVAILABILITY_PATH).authenticated()
            .anyExchange().authenticated())
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .oauth2ResourceServer(oauth2 -> oauth2
            .bearerTokenConverter(bearerTokenConverter())
            .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())))
        .build();
  }

  private ServerAuthenticationConverter bearerTokenConverter() {
    ServerBearerTokenAuthenticationConverter converter =
        new ServerBearerTokenAuthenticationConverter();
    converter.setAllowUriQueryParameter(true);
    return converter;
  }

  private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
    return jwt -> {
      List<String> roles = jwt.getClaimAsStringList("roles");
      Collection<SimpleGrantedAuthority> authorities = roles == null
          ? List.of()
          : roles.stream().map(SimpleGrantedAuthority::new).toList();
      return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    };
  }
}
