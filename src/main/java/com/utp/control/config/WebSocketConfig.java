package com.utp.control.config;

import com.utp.control.expose.websocket.AvailabilityWebSocketHandler;
import com.utp.control.util.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

@Configuration
public class WebSocketConfig {

  @Bean
  HandlerMapping availabilityWebSocketHandlerMapping(
      AvailabilityWebSocketHandler availabilityWebSocketHandler) {
    Map<String, WebSocketHandler> handlers =
        Map.of(Constants.WEBSOCKET_AVAILABILITY_PATH, availabilityWebSocketHandler);

    SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
    handlerMapping.setUrlMap(handlers);
    handlerMapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return handlerMapping;
  }

  @Bean
  WebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new WebSocketHandlerAdapter();
  }
}
