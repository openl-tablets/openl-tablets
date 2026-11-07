package org.openl.binding.impl;

import java.util.Locale;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.openl.binding.IBoundNode;
import org.openl.rules.binding.MessageSourceResourceMethod;
import org.openl.util.text.TextInfo;

/**
 * Finds the key a call of {@code msg} writes as text, as {@code say.hello} in {@code msg("say.hello")}.
 *
 * <p>The hint gives the default text of the message, or tells that the default localization file has no such key.
 * A key built at runtime is not marked.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MessageKeyNodeUsageCreator implements NodeUsageCreator {

    @Override
    public boolean accept(IBoundNode boundNode) {
        return boundNode instanceof MethodBoundNode call && call.getMethodCaller() != null
                && MessageSourceResourceMethod.calledBy(call.getMethodCaller()) != null;
    }

    @Override
    public Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startIndex) {
        var call = (MethodBoundNode) boundNode;
        var method = MessageSourceResourceMethod.calledBy(call.getMethodCaller());
        var literal = MessageSourceResourceMethod.keyOf(call.getChildren());
        var location = literal == null ? null : literal.getSyntaxNode().getSourceLocation();
        if (method == null || location == null || !location.isTextLocation()) {
            return Optional.empty();
        }
        var key = (String) literal.getValue();
        var text = new TextInfo(sourceString);
        // The location covers the quotes as well, and ends inclusively on the closing one.
        var start = startIndex + location.getStart().getAbsolutePosition(text) + 1;
        var end = startIndex + location.getEnd().getAbsolutePosition(text);
        var defaults = method.getMessageSource().getMessageBundle(Locale.ROOT);
        var description = defaults.has(key) ? key + " = " + defaults.msg(key)
                : key + " is not in the default localization file";
        return Optional.of(new MessageKeyUsage(start, end, key, description));
    }

    private static class Holder {
        private static final MessageKeyNodeUsageCreator INSTANCE = new MessageKeyNodeUsageCreator();
    }

    static MessageKeyNodeUsageCreator getInstance() {
        return Holder.INSTANCE;
    }
}
