package org.openl.binding.impl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * The key of a message, written as text in a call of {@code msg}.
 *
 * <p>The usage covers the key without its quotes. It refers to no table: a screen follows it to the key among the
 * localization files of the project.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class MessageKeyUsage implements NodeUsage {

    private final int start;

    /** The ending index, exclusive. */
    private final int end;

    /** The key, as the call writes it. */
    private final String key;

    /** The default text of the message, or a note that the default localization file has none. */
    private final String description;

    @Override
    public @Nullable String getUri() {
        return null;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.MESSAGE;
    }
}
