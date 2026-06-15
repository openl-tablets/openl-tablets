package org.openl.studio.socket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authorization.AuthorizationManager;

class WebSocketConfigTest {

    @SuppressWarnings("unchecked")
    private final WebSocketConfig config = new WebSocketConfig(mock(ApplicationContext.class), new ObjectMapper(),
            mock(AuthorizationManager.class));

    @Test
    void heartbeatSchedulerIsDaemonSoItDoesNotBlockJvmShutdown() {
        ThreadPoolTaskScheduler scheduler = config.messageBrokerTaskScheduler();

        assertTrue(scheduler.isDaemon(), "heartbeat thread must be daemon to let the JVM exit after the server stops");
        assertEquals("ws-heartbeat-thread-", scheduler.getThreadNamePrefix());
    }

    @Test
    void simpleBrokerUsesTheDaemonHeartbeatScheduler() {
        var registry = mock(MessageBrokerRegistry.class);
        var brokerRegistration = mock(SimpleBrokerRegistration.class);
        when(registry.enableSimpleBroker("/topic", "/queue")).thenReturn(brokerRegistration);

        config.configureMessageBroker(registry);

        var scheduler = ArgumentCaptor.forClass(TaskScheduler.class);
        verify(brokerRegistration).setTaskScheduler(scheduler.capture());
        assertTrue(scheduler.getValue() instanceof ThreadPoolTaskScheduler tps && tps.isDaemon(),
                "broker must use the managed daemon heartbeat scheduler");
        verify(registry).setUserDestinationPrefix("/user");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }
}
