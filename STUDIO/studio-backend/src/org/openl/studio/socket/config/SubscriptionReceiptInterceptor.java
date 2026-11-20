package org.openl.studio.socket.config;

import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

import org.openl.studio.socket.StatusSequence;

/**
 * Answers a SUBSCRIBE frame that asks for a receipt once the subscription is registered.
 *
 * <p>A client reads the current state over REST and follows its changes over the socket. A change told before its
 * subscription is registered reaches nobody, so the client needs to know when that happened. The receipt tells
 * it, and carries in the {@value #SEQUENCE_HEADER} header the {@link StatusSequence} number of that moment: a state
 * the client read under a smaller number may have changed unheard, and is read again.
 *
 * <p>The receipt follows the handling of the frame by the handler of its destination: the broker for a broker
 * destination, or the user-destination handler, which hands the frame on to the broker in the same call. A
 * subscription to an application destination, or one that failed, gets no receipt. The simple broker answers the
 * receipt of a DISCONNECT frame only.
 */
@RequiredArgsConstructor
final class SubscriptionReceiptInterceptor implements ExecutorChannelInterceptor {

    /** The header of the receipt that holds the sequence number the subscription started at. */
    static final String SEQUENCE_HEADER = "status-sequence";

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    private final Supplier<MessageChannel> clientOutboundChannel;
    private final StatusSequence statusSequence;
    /** The prefix of the user destinations, ending with a slash. */
    private final String userDestinationPrefix;

    @Override
    public void afterMessageHandled(Message<?> message,
                                    MessageChannel channel,
                                    MessageHandler handler,
                                    @Nullable Exception ex) {
        if (ex != null || SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.SUBSCRIBE) {
            return;
        }
        var frame = StompHeaderAccessor.wrap(message);
        var receipt = frame.getReceipt();
        var destination = frame.getDestination();
        if (receipt != null && destination != null && registers(handler, destination)) {
            var answer = StompHeaderAccessor.create(StompCommand.RECEIPT);
            answer.setReceiptId(receipt);
            answer.setSessionId(frame.getSessionId());
            answer.setNativeHeader(SEQUENCE_HEADER, Long.toString(statusSequence.next()));
            answer.setLeaveMutable(true);
            clientOutboundChannel.get().send(MessageBuilder.createMessage(EMPTY_PAYLOAD, answer.getMessageHeaders()));
        }
    }

    /** Whether the handler is the one that registers a subscription to the destination. */
    private boolean registers(MessageHandler handler, String destination) {
        var user = destination.startsWith(userDestinationPrefix);
        return switch (handler) {
            case UserDestinationMessageHandler ignored -> user;
            case AbstractBrokerMessageHandler broker -> !user && broker.getDestinationPrefixes()
                    .stream()
                    .anyMatch(destination::startsWith);
            default -> false;
        };
    }
}
