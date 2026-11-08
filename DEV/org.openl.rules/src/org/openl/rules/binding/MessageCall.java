package org.openl.rules.binding;

import org.openl.syntax.ISyntaxNode;

/**
 * A call of the built-in {@code msg} that writes its key as text, such as {@code msg("order.total", total)}.
 *
 * @param key     the key, as the call writes it
 * @param keyNode the text of the key in the call
 * @param node    the whole call
 * @param values  how many values the call passes, or 0 when it passes none or it cannot be told how many
 */
public record MessageCall(String key, ISyntaxNode keyNode, ISyntaxNode node, int values) {
}
