package org.openl.studio.projects.service.localization;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.util.HashingUtils;
import org.openl.util.PropertiesUtils;

/**
 * A localization file as it was read: the language it is written for, and its messages in the order they are written,
 * each with the bytes it takes in the file.
 *
 * <p>A message is read as the engine reads it ({@link PropertiesUtils}): only an equals sign or a colon ends a key, a
 * line without one is a key with no text, a line starting with {@code #} or {@code !} is a comment, and a backslash at
 * the end of a line continues the message on the next one.
 *
 * <p>A file that is not UTF-8 is read in the legacy encoding of Windows, the way a user wrote it. It is written in
 * UTF-8, whole, the first time it is written.
 *
 * @param tag       the language, country and variant of the file name joined by {@code _}; empty for the default file
 * @param path      where the file is
 * @param stamp     the size and the time of the last change of the file when it was read
 * @param digest    the digest of the content read, which tells a change the size and the time cannot
 * @param legacy    whether the file is not UTF-8
 * @param lineBreak the line break the file is written with, which a message added to it takes
 * @param messages  the messages in the order they are written, a key written twice included
 * @param texts     the text of every key, the last one written winning, as the engine reads it: {@code null} for a
 *                  key written without a text
 */
public record MessageFile(String tag,
                          Path path,
                          Stamp stamp,
                          String digest,
                          boolean legacy,
                          String lineBreak,
                          List<Message> messages,
                          Map<String, @Nullable String> texts) {

    static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    /**
     * One message of the file.
     *
     * @param key        the key
     * @param text       the text, or {@code null} for a line without a separator: the engine reads no text for its key
     * @param separator  what is written between the key and the text, such as {@code " = "} or {@code ":"}; empty
     *                   for a line without a separator
     * @param start      the first byte of the first line of the message
     * @param contentEnd the end of its last line, its line break left out
     * @param end        the end of its last line, its line break included
     */
    public record Message(String key, @Nullable String text, String separator, long start, long contentEnd, long end) {
    }

    /** The separator of a message added to a file that has none to follow. */
    static final String DEFAULT_SEPARATOR = "=";

    /**
     * The separator a message added to the file takes, so it is written as the file writes its messages: that of the
     * last message written with one, or {@link #DEFAULT_SEPARATOR} when none is.
     */
    String separator() {
        return messages.reversed()
                .stream()
                .map(Message::separator)
                .filter(written -> !written.isEmpty())
                .findFirst()
                .orElse(DEFAULT_SEPARATOR);
    }

    /** The size of a file and the time of its last change, which tell whether it was written since it was read. */
    public record Stamp(long size, long modified) {

        static @Nullable Stamp of(Path path) {
            try {
                var attributes = Files.readAttributes(path, BasicFileAttributes.class);
                return new Stamp(attributes.size(), attributes.lastModifiedTime().toMillis());
            } catch (IOException e) {
                return null;
            }
        }
    }

    /**
     * Reads a localization file.
     *
     * @param path the file
     * @param tag  the language it is written for
     * @return the file as read
     */
    public static MessageFile read(Path path, String tag) {
        try {
            var stamp = Stamp.of(path);
            var bytes = Files.readAllBytes(path);
            var legacy = !isUtf8(bytes);
            var lines = new Lines(bytes, legacy ? WINDOWS_1252 : StandardCharsets.UTF_8);
            lines.read();
            var texts = new LinkedHashMap<String, @Nullable String>();
            lines.messages.forEach(message -> texts.put(message.key(), message.text()));
            return new MessageFile(tag,
                    path,
                    stamp == null ? new Stamp(bytes.length, 0) : stamp,
                    HashingUtils.sha256Hex(bytes),
                    legacy,
                    lines.lineBreak == null ? "\n" : lines.lineBreak,
                    List.copyOf(lines.messages),
                    Collections.unmodifiableMap(texts));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * Splits a file into its messages: the lines of the file, each joined with those it continues on, read one by
     * one as the engine reads them.
     */
    private static final class Lines {

        private final byte[] bytes;
        private final Charset charset;
        private final List<Message> messages = new ArrayList<>();
        private @Nullable String lineBreak;
        /** The lines of the message being read, and where it starts; negative between two messages. */
        private final StringBuilder logical = new StringBuilder();
        private long start = -1;
        private boolean comment;

        Lines(byte[] bytes, Charset charset) {
            this.bytes = bytes;
            this.charset = charset;
        }

        void read() {
            var at = 0;
            while (at < bytes.length) {
                var contentEnd = at;
                while (contentEnd < bytes.length && !isLineBreak(bytes[contentEnd])) {
                    contentEnd++;
                }
                var next = afterLineBreak(contentEnd);
                readLine(at, contentEnd, next);
                at = next;
            }
            if (start >= 0) {
                // The last line continues past the end of the file.
                take(bytes.length, bytes.length);
            }
        }

        private int afterLineBreak(int contentEnd) {
            if (contentEnd >= bytes.length) {
                return contentEnd;
            }
            var crlf = bytes[contentEnd] == '\r' && contentEnd + 1 < bytes.length && bytes[contentEnd + 1] == '\n';
            var next = crlf ? contentEnd + 2 : contentEnd + 1;
            if (lineBreak == null) {
                lineBreak = new String(bytes, contentEnd, next - contentEnd, StandardCharsets.US_ASCII);
            }
            return next;
        }

        private void readLine(int from, int contentEnd, int next) {
            var content = new String(bytes, from, contentEnd - from, charset);
            if (start < 0) {
                start = from;
                comment = isComment(content);
            } else {
                logical.append('\n');
            }
            logical.append(content);
            if (comment || !continues(content)) {
                take(contentEnd, next);
            }
        }

        /** Reads the message whose lines were gathered, unless they are a comment or blank. */
        private void take(long contentEnd, long end) {
            if (!comment) {
                try {
                    for (var entry : PropertiesUtils.readEntries(new StringReader(logical.toString()))) {
                        messages.add(new Message(entry.key(),
                                entry.value(),
                                separatorOf(logical),
                                start,
                                contentEnd,
                                end));
                    }
                } catch (IOException e) {
                    // A truncated escape at the end of the file: the engine fails on it too, and reads nothing of it.
                }
            }
            logical.setLength(0);
            start = -1;
        }

        /**
         * What a message writes between its key and its text, as it is written: the blanks before the first equals
         * sign or colon that is not escaped, the sign, and the blanks after it. Empty for a message without one.
         */
        static String separatorOf(CharSequence message) {
            var at = signAt(message);
            if (at < 0) {
                return "";
            }
            var keyEnd = at;
            while (keyEnd > 0 && isBlank(message.charAt(keyEnd - 1))) {
                keyEnd--;
            }
            var textStart = at + 1;
            while (textStart < message.length() && isBlank(message.charAt(textStart))) {
                textStart++;
            }
            return message.subSequence(keyEnd, textStart).toString();
        }

        /** Where the first equals sign or colon that is not escaped is, or -1 when there is none. */
        private static int signAt(CharSequence message) {
            var at = 0;
            while (at < message.length() && message.charAt(at) != '=' && message.charAt(at) != ':') {
                at += message.charAt(at) == '\\' ? 2 : 1;
            }
            return at < message.length() ? at : -1;
        }

        private static boolean isBlank(char ch) {
            return ch == ' ' || ch == '\t' || ch == '\f';
        }

        /** Whether the first character of a line that is no whitespace starts a comment. */
        private static boolean isComment(String line) {
            for (var i = 0; i < line.length(); i++) {
                var ch = line.charAt(i);
                if (!Character.isWhitespace(ch)) {
                    return ch == '#' || ch == '!';
                }
            }
            return false;
        }

        /** Whether a line ends with a backslash that is not escaped itself: the message goes on in the next one. */
        private static boolean continues(String line) {
            var backslashes = 0;
            for (var i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
                backslashes++;
            }
            return backslashes % 2 == 1;
        }

        private static boolean isLineBreak(byte b) {
            return b == '\n' || b == '\r';
        }
    }
}
