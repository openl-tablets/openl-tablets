package org.openl.studio.socket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.openl.studio.socket.StatusSequence;

class SubscriptionReceiptInterceptorTest {

    private static final String USER_STATUS = "/user/topic/projects/p1/status";
    private static final String BROADCAST = "/topic/public/notification.txt";

    private final List<Message<?>> sent = Collections.synchronizedList(new ArrayList<>());
    private final MessageChannel outbound = (message, timeout) -> sent.add(message);
    private final StatusSequence sequence = new StatusSequence();
    private final SubscriptionReceiptInterceptor interceptor =
            new SubscriptionReceiptInterceptor(() -> outbound, sequence, "/user/");

    private final MessageChannel inbound = mock(MessageChannel.class);
    private final SimpleBrokerMessageHandler broker =
            new SimpleBrokerMessageHandler(new ExecutorSubscribableChannel(), outbound,
                    new ExecutorSubscribableChannel(), List.of("/topic", "/queue"));
    private final UserDestinationMessageHandler users = new UserDestinationMessageHandler(
            new ExecutorSubscribableChannel(), new ExecutorSubscribableChannel(),
            new DefaultUserDestinationResolver(mock(SimpUserRegistry.class)));

    @Test
    void aSubscriptionToAUserDestinationIsAnsweredOnceTheUserDestinationHandlerRegisteredIt() {
        var frame = subscribe(USER_STATUS, "r-1");

        interceptor.afterMessageHandled(frame, inbound, broker, null);
        assertTrue(sent.isEmpty(), "the broker ignores a user destination: it registers nothing");

        interceptor.afterMessageHandled(frame, inbound, users, null);

        var receipt = StompHeaderAccessor.wrap(sent.getFirst());
        assertEquals(StompCommand.RECEIPT, receipt.getCommand());
        assertEquals("r-1", receipt.getReceiptId());
        assertEquals("session", receipt.getSessionId());
        assertTrue(Long.parseLong(receipt.getFirstNativeHeader(SubscriptionReceiptInterceptor.SEQUENCE_HEADER)) > 0);
        assertEquals(1, sent.size());
    }

    @Test
    void aSubscriptionToABrokerDestinationIsAnsweredOnceTheBrokerRegisteredIt() {
        var frame = subscribe(BROADCAST, "r-2");

        interceptor.afterMessageHandled(frame, inbound, users, null);
        assertTrue(sent.isEmpty(), "the user-destination handler ignores a broker destination");

        interceptor.afterMessageHandled(frame, inbound, broker, null);

        assertEquals("r-2", StompHeaderAccessor.wrap(sent.getFirst()).getReceiptId());
    }

    @Test
    void theReceiptCarriesTheSequenceOfTheMomentTheSubscriptionStarted() {
        var before = sequence.next();

        interceptor.afterMessageHandled(subscribe(USER_STATUS, "r-3"), inbound, users, null);

        var started = Long.parseLong(StompHeaderAccessor.wrap(sent.getFirst())
                .getFirstNativeHeader(SubscriptionReceiptInterceptor.SEQUENCE_HEADER));
        assertTrue(started > before);
        assertTrue(sequence.next() > started);
    }

    @Test
    void nothingIsAnsweredWhereNoReceiptIsAskedForOrNoSubscriptionWasMade() {
        // No receipt asked for.
        interceptor.afterMessageHandled(subscribe(USER_STATUS, null), inbound, users, null);
        // The subscription failed.
        interceptor.afterMessageHandled(subscribe(USER_STATUS, "r-4"), inbound, users, new IllegalStateException());
        // An application destination: no broker registers it.
        interceptor.afterMessageHandled(subscribe("/app/public/notification.txt", "r-5"), inbound, broker, null);
        interceptor.afterMessageHandled(subscribe("/app/public/notification.txt", "r-5"), inbound, users, null);
        // Any other handler.
        interceptor.afterMessageHandled(subscribe(USER_STATUS, "r-6"), inbound, mock(MessageHandler.class), null);
        // A frame other than a subscription asking for a receipt.
        var send = StompHeaderAccessor.create(StompCommand.SEND);
        send.setDestination(USER_STATUS);
        send.setReceipt("r-7");
        send.setSessionId("session");
        interceptor.afterMessageHandled(MessageBuilder.createMessage(new byte[0], send.getMessageHeaders()),
                inbound, users, null);

        assertTrue(sent.isEmpty());
    }

    @Test
    void aStatusPublishedOnceTheReceiptArrivedReachesTheSubscription() {
        var pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(4);
        pool.initialize();
        // As the server wires its channels: the frames of the clients are handled on a pool, the broker channel
        // hands a frame on in the same call.
        var clientInbound = new ExecutorSubscribableChannel(pool);
        var brokerChannel = new ExecutorSubscribableChannel();
        var delivered = Collections.synchronizedList(new ArrayList<String>());
        MessageChannel clientOutbound = (message, timeout) -> {
            var headers = StompHeaderAccessor.wrap(message);
            if (headers.getCommand() == StompCommand.RECEIPT) {
                // The client reads the status as soon as its subscription is answered; the change right after it
                // must reach the subscription.
                brokerChannel.send(status("/topic/projects/p1/status-usersession", "ok"));
            } else if (headers.getMessageType() == SimpMessageType.MESSAGE) {
                delivered.add(new String((byte[]) message.getPayload(), StandardCharsets.UTF_8));
            }
            return true;
        };
        var simpleBroker = new SimpleBrokerMessageHandler(clientInbound, clientOutbound, brokerChannel,
                List.of("/topic", "/queue"));
        var userHandler = new UserDestinationMessageHandler(clientInbound, brokerChannel,
                new DefaultUserDestinationResolver(mock(SimpUserRegistry.class)));
        clientInbound.setInterceptors(List.of(new SubscriptionReceiptInterceptor(() -> clientOutbound,
                sequence, "/user/")));
        simpleBroker.start();
        userHandler.start();
        try {
            var connect = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
            connect.setSessionId("session");
            simpleBroker.handleMessage(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()));

            clientInbound.send(subscribe(USER_STATUS, "r-8"));

            Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> delivered.contains("ok"));
        } finally {
            userHandler.stop();
            simpleBroker.stop();
            pool.shutdown();
        }
    }

    private static Message<byte[]> subscribe(String destination, @Nullable String receipt) {
        var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setDestination(destination);
        headers.setSubscriptionId("subscription");
        headers.setSessionId("session");
        if (receipt != null) {
            headers.setReceipt(receipt);
        }
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private static Message<byte[]> status(String destination, String status) {
        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setDestination(destination);
        return MessageBuilder.createMessage(status.getBytes(StandardCharsets.UTF_8), headers.getMessageHeaders());
    }
}
