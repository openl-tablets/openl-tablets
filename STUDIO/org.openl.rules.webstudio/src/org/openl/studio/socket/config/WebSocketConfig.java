package org.openl.studio.socket.config;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextPropagationChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import org.openl.studio.socket.handler.AnonymousSupportHandshakeHandler;
import org.openl.studio.socket.handler.AnonymouseSecurityContextHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final AuthorizationManager<Message<?>> messageAuthorizationManager;

    public WebSocketConfig(ApplicationContext applicationContext,
                           ObjectMapper objectMapper,
                           AuthorizationManager<Message<?>> messageAuthorizationManager) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        this.messageAuthorizationManager = messageAuthorizationManager;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // enables ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new AnonymousSupportHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor(),  // pass the HTTP Session to the WebSocket
                        new AnonymouseSecurityContextHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
        te.setPoolSize(1);
        te.setThreadNamePrefix("ws-heartbeat-thread-");
        te.initialize();
        registry.enableSimpleBroker("/topic", "/queue") // for broadcast messages to all subscribers
                .setTaskScheduler(te);
        registry.setUserDestinationPrefix("/user"); // for user-specific destinations
        registry.setApplicationDestinationPrefixes("/app"); // prefix for messages sent to the server
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        var authInterceptor = new AuthorizationChannelInterceptor(messageAuthorizationManager);
        var publisher = new SpringAuthorizationEventPublisher(applicationContext);
        authInterceptor.setAuthorizationEventPublisher(publisher);
        registration.interceptors(getSessionSecurityContextChannelInterceptor(),
                new SecurityContextPropagationChannelInterceptor(),
                authInterceptor);
    }

    // Resolve the security context from the session attributes
    @Bean
    public ChannelInterceptor getSessionSecurityContextChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null && sessionAttributes.containsKey(SPRING_SECURITY_CONTEXT_KEY)) {
                        SecurityContext securityContext = (SecurityContext) sessionAttributes.get(SPRING_SECURITY_CONTEXT_KEY);

                        if (securityContext != null && securityContext.getAuthentication() != null
                                && securityContext.getAuthentication().isAuthenticated()) {
                            SecurityContextHolder.setContext(securityContext);
                            accessor.setUser(securityContext.getAuthentication());
                        }
                    }
                }
                return message;
            }
        };
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new StringMessageConverter());
        messageConverters.add(new ByteArrayMessageConverter());
        messageConverters.add(new MappingJackson2MessageConverter(this.objectMapper));
        return true;
    }
}
