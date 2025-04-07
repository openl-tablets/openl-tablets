package org.openl.studio.socket.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
public class WebSocketSecurityConfig {

    @Bean
    @ConditionalOnExpression("'${user.mode}' != 'single'")
    public AuthorizationManager<Message<?>> messageAuthorizationManager() {
        return MessageMatcherDelegatingAuthorizationManager.builder()
                // Allow unauthenticated users to connect to the WebSocket server
                .nullDestMatcher().permitAll()
                // Grant access to the specific destinations
                .simpSubscribeDestMatchers(
                        "/topic/public/**",
                        "/app/public/**",
                        "/user/queue/errors"
                ).permitAll()
                // Authentication is required to any other messages.
                .anyMessage().authenticated()
                .build();
    }

    @Bean
    @ConditionalOnExpression("'${user.mode}' == 'single'")
    public AuthorizationManager<Message<?>> noMessageAuthorizationManager() {
        return MessageMatcherDelegatingAuthorizationManager.builder()
                .nullDestMatcher().permitAll()
                .anyMessage().permitAll()
                .build();
    }

}
