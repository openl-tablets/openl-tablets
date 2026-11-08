package org.openl.studio.projects.service.localization;

import org.jspecify.annotations.Nullable;

import org.openl.util.PropertiesUtils;

/**
 * Writes a message of a localization file so that the engine reads it back as it is ({@link PropertiesUtils}).
 *
 * <p>A line break, a tab and a backslash are escaped. So is a whitespace at either end of a text, which reading
 * drops, and a separator in a key, which would end it. A key without a text is written without a separator.
 */
final class PropertiesText {

    private PropertiesText() {
    }

    /**
     * The line of a message, without its line break.
     *
     * @param key       the key
     * @param text      the text, or {@code null} for a key with no text at all
     * @param separator what is written between the key and the text, such as {@code " = "}
     */
    static String line(String key, @Nullable String text, String separator) {
        var written = escapeKey(key);
        return text == null ? written : written + separator + escapeText(text);
    }

    private static String escapeKey(String key) {
        var escaped = escapeText(key).replace(":", "\\:").replace("=", "\\=");
        return escaped.startsWith("#") || escaped.startsWith("!") ? "\\" + escaped : escaped;
    }

    private static String escapeText(String text) {
        var escaped = text.replace("\\", "\\\\")
                .replace("\f", "\\f")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return escapeEnds(escaped);
    }

    /**
     * Escapes the whitespaces reading drops: the first one of a text, which is skipped with the separator, and every
     * one at its end.
     */
    private static String escapeEnds(String text) {
        var end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        var written = new StringBuilder();
        var body = text.substring(0, end);
        if (!body.isEmpty() && Character.isWhitespace(body.charAt(0))) {
            written.append(escapeWhitespace(body.charAt(0))).append(body, 1, body.length());
        } else {
            written.append(body);
        }
        text.substring(end).chars().forEach(ch -> written.append(escapeWhitespace((char) ch)));
        return written.toString();
    }

    private static String escapeWhitespace(char ch) {
        return ch == ' ' ? "\\ " : "\\u%04x".formatted((int) ch);
    }
}
