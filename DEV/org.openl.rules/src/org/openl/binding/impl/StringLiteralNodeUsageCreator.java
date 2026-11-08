package org.openl.binding.impl;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.openl.binding.IBoundNode;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * Finds the texts an expression writes in quotes, as {@code min} in {@code result == "min"}.
 *
 * <p>Each text is one usage over the characters between its quotes. A text built at runtime is not one, and neither
 * are words a list binds into a text, which are written without quotes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class StringLiteralNodeUsageCreator implements NodeUsageCreator {

    @Override
    public boolean accept(IBoundNode boundNode) {
        return boundNode instanceof LiteralBoundNode literal && literal.getValue() instanceof String;
    }

    @Override
    public Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startIndex) {
        var literal = (LiteralBoundNode) boundNode;
        return Optional.ofNullable(literal.getSyntaxNode().getSourceLocation())
                .filter(ILocation::isTextLocation)
                .flatMap(location -> usageOf(literal, location, sourceString, startIndex));
    }

    private static Optional<NodeUsage> usageOf(LiteralBoundNode literal, ILocation location, String sourceString,
                                               int startIndex) {
        var text = new TextInfo(sourceString);
        // The location covers the quotes as well, and ends inclusively on the closing one.
        var opening = location.getStart().getAbsolutePosition(text);
        if (opening < 0 || opening >= sourceString.length() || sourceString.charAt(opening) != '"') {
            return Optional.empty();
        }
        var start = startIndex + opening + 1;
        var end = startIndex + location.getEnd().getAbsolutePosition(text);
        return Optional.of(new StringLiteralUsage(start, end, (String) literal.getValue(), literal.getSyntaxNode()));
    }

    private static class Holder {
        private static final StringLiteralNodeUsageCreator INSTANCE = new StringLiteralNodeUsageCreator();
    }

    static StringLiteralNodeUsageCreator getInstance() {
        return Holder.INSTANCE;
    }
}
