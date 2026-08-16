package com.utp.control.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  @LoadBalanced
  WebClient.Builder loadBalancedWebClientBuilder() {
    return WebClient.builder();
  }

  @Bean
  WebClient usersWebClient(@LoadBalanced WebClient.Builder webClientBuilder,
                           @Value("${users-service.base-url}") String usersServiceBaseUrl) {
    return webClientBuilder
        .baseUrl(usersServiceBaseUrl)
        .filter(new ServerBearerExchangeFilterFunction())
        .build();
  }

  @Bean
  WebClient parkingRequestWebClient(
      @LoadBalanced WebClient.Builder webClientBuilder,
      @Value("${parking-request-service.base-url}") String parkingRequestServiceBaseUrl) {
    return webClientBuilder
        .baseUrl(parkingRequestServiceBaseUrl)
        .filter(new ServerBearerExchangeFilterFunction())
        .build();
  }
}
