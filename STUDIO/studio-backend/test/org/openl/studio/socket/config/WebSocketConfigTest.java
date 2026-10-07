package org.openl.studio.socket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authorization.AuthorizationManager;

class WebSocketConfigTest {

    private static final String STATUS_TOPIC = "/topic/projects/p1/status";

    @SuppressWarnings("unchecked")
    private final WebSocketConfig config = new WebSocketConfig(mock(ApplicationContext.class), new ObjectMapper(),
            mock(AuthorizationManager.class));

    @Test
    void heartbeatSchedulerIsDaemonSoItDoesNotBlockJvmShutdown() {
        var scheduler = config.messageBrokerTaskScheduler();

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

    @Test
    void statusesReachASessionInTheOrderTheyWerePublished() {
        var pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(4);
        pool.initialize();
        // As the server hands messages to its sessions: each on a thread of the pool.
        var outbound = new ExecutorSubscribableChannel(pool);
        var delivered = Collections.synchronizedList(new ArrayList<String>());
        var ended = new CountDownLatch(1);
        outbound.subscribe(message -> {
            if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) {
                // The broker's own answers — the session's connect acknowledged, heartbeats — are not statuses.
                return;
            }
            var status = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
            if ("compiling".equals(status)) {
                // The thread handing over the progress lags behind, as one does on a loaded server: a status
                // published after it has that long to overtake it.
                awaitQuietly(ended);
            } else {
                ended.countDown();
            }
            delivered.add(status);
        });
        var registry = new Registry(new ExecutorSubscribableChannel(), outbound);
        config.configureMessageBroker(registry);
        var broker = registry.broker();
        // The heartbeat scheduler is a bean the context initializes; built here by hand, it is initialized here.
        var heartbeats = (ThreadPoolTaskScheduler) broker.getTaskScheduler();
        heartbeats.initialize();
        broker.start();
        try {
            broker.handleMessage(session(SimpMessageType.CONNECT));
            broker.handleMessage(session(SimpMessageType.SUBSCRIBE));
            // A compilation of one module ends a moment after it reports the module compiled.
            broker.handleMessage(status("compiling"));
            broker.handleMessage(status("ok"));

            Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> delivered.size() == 2);
            // The last status a screen hears is the one it shows: heard the other way round, a compilation
            // that ended would be shown compiling for good.
            assertEquals(List.of("compiling", "ok"), delivered);
        } finally {
            broker.stop();
            heartbeats.shutdown();
            pool.shutdown();
        }
    }

    /** A frame of the one session the test follows: its connect, or its subscription to the status topic. */
    private static Message<byte[]> session(SimpMessageType type) {
        var headers = SimpMessageHeaderAccessor.create(type);
        headers.setSessionId("session");
        headers.setSubscriptionId("subscription");
        headers.setDestination(STATUS_TOPIC);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private static Message<byte[]> status(String status) {
        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setDestination(STATUS_TOPIC);
        return MessageBuilder.createMessage(status.getBytes(StandardCharsets.UTF_8), headers.getMessageHeaders());
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(300, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** The registry the configuration fills in, asked for the broker it describes. */
    private static final class Registry extends MessageBrokerRegistry {

        Registry(SubscribableChannel inbound, MessageChannel outbound) {
            super(inbound, outbound);
        }

        SimpleBrokerMessageHandler broker() {
            return Objects.requireNonNull(getSimpleBroker(new ExecutorSubscribableChannel()));
        }
    }
}
