package org.openl.rules.maven.migration;

import java.util.BitSet;
import java.util.List;

/**
 * Reads where the code of a Groovy source is, so a migration rewrites the code and leaves the texts and the comments
 * as they are.
 * <p>
 * A text is a string in single, double or triple quotes. The code inside the {@code ${}} of a string counts as text
 * too, and so does a slashy string, which reads like a division.
 *
 * @author Yury Molchan
 */
final class GroovyText {

    private static final List<String> QUOTES = List.of("'''", "\"\"\"", "'", "\"");

    private GroovyText() {
    }

    /**
     * Returns the positions of the source that are in a text or in a comment.
     */
    static BitSet literals(String source) {
        var literals = new BitSet(source.length());
        var i = 0;
        while (i < source.length()) {
            var end = literalEnd(source, i);
            literals.set(i, end);
            i = Math.max(end, i + 1);
        }
        return literals;
    }

    /**
     * Returns the position after the text or the comment that starts at the position, or the position itself when
     * none starts there.
     */
    private static int literalEnd(String source, int i) {
        if (source.startsWith("//", i)) {
            var eol = source.indexOf('\n', i);
            return eol < 0 ? source.length() : eol;
        } else if (source.startsWith("/*", i)) {
            var end = source.indexOf("*/", i + 2);
            return end < 0 ? source.length() : end + 2;
        }
        var quote = QUOTES.stream().filter(q -> source.startsWith(q, i)).findFirst().orElse(null);
        return quote == null ? i : closing(source, i + quote.length(), quote);
    }

    /**
     * Returns the position after the quote that closes the text, skipping the escaped characters.
     */
    private static int closing(String source, int from, String quote) {
        var j = from;
        while (j < source.length() && !source.startsWith(quote, j)) {
            j += source.charAt(j) == '\\' ? 2 : 1;
        }
        return Math.min(j + quote.length(), source.length());
    }
}
