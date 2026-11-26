package com.wsk.controller.webSocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Created by wsk1103 on 2017/5/22.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        //WebIM WebSocket通道
        registry.addHandler(chatWebSocketHandler(), "/webSocketIMServer").setAllowedOriginPatterns("*");
        registry.addHandler(chatWebSocketHandler(), "/sockjs/webSocketIMServer").setAllowedOriginPatterns("*");
        registry.addHandler(chatWebSocketHandler(), "/sockjs/webSocketIMServer").setAllowedOriginPatterns("*").withSockJS();
    }
    @Bean
    public ChatWebSocketHandler chatWebSocketHandler() {
        return new ChatWebSocketHandler();
    }
}
