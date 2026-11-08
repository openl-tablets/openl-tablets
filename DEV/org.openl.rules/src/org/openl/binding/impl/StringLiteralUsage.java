package org.openl.binding.impl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.syntax.ISyntaxNode;

/**
 * A text written in quotes in an expression, such as {@code "order.total"} in {@code msg("order.total")}.
 *
 * <p>The usage covers the text without its quotes. It means nothing by itself: a screen tells what the text stands
 * for, such as the key of a message, and leaves the other texts unmarked.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class StringLiteralUsage implements NodeUsage {

    private final int start;

    /** The ending index, exclusive. */
    private final int end;

    /** The text, as the expression writes it between the quotes. */
    private final String value;

    /** The literal in the expression, which tells it from the same text written elsewhere. */
    @EqualsAndHashCode.Exclude
    private final ISyntaxNode syntaxNode;

    @Override
    public String getDescription() {
        return value;
    }

    @Override
    public @Nullable String getUri() {
        return null;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.OTHER;
    }
}
