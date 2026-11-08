package org.openl.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * An utility for working with properties files. Parsing, storing.
 *
 * @author Yury Molchan
 */
public final class PropertiesUtils {

    private PropertiesUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Loads properties in the similar manner, like in {@link java.util.Properties}. The difference is in processing whitespaces in keys.
     * This implementation keeps all whitespaces inside keys. For example
     * <code>
     * " A some key = A some value "   =>   key="A some key" , value="A some value "
     * </code>
     *
     * @param input  a reader of properties
     * @param result a target function, where key/value properties pair will be sent
     * @see java.util.Properties
     */
    public static void load(Reader input, BiConsumer<? super String, ? super String> result) throws IOException {
        new Parser(input, false, (key, value, line, places) -> result.accept(key, value)).parse();
    }

    /**
     * One key/value pair of a properties file and the line it starts on.
     *
     * @param key    the key, as {@link #load(Reader, BiConsumer)} reads it
     * @param value  the value, or {@code null} for a line without a separator
     * @param line   the number of the line the pair starts on, the first line being 1
     * @param places where the characters of the key and the value are in the text, when they were asked for
     */
    public record Entry(String key, @Nullable String value, int line, Places places) {

        public Entry(String key, @Nullable String value, int line) {
            this(key, value, line, Places.NONE);
        }
    }

    /**
     * Where the characters of the key and of the value of a pair are in the text they were read from.
     *
     * <p>An escaped character takes the place of all it is written with, as {@code \=} or {@code \u00e9}, and a
     * value continued on the next line goes on there. Nothing is known of a pair read without its places.
     */
    public static final class Places {

        private static final Places NONE = new Places(new int[0], new int[0]);

        /** Four numbers for each character: its line and column, and the line and column after it. */
        private final int[] key;
        private final int[] value;

        private Places(int[] key, int[] value) {
            this.key = key;
            this.value = value;
        }

        /** Where the key is written, or {@code null} when its places were not read. */
        public @Nullable TextRange ofKey() {
            return rangeOf(key, 0, key.length / 4);
        }

        /**
         * Where a piece of the value is written, or {@code null} when its places were not read.
         *
         * @param start the position of the first character of the piece in the value
         * @param end   the position after its last character
         */
        public @Nullable TextRange ofValue(int start, int end) {
            return rangeOf(value, start, end);
        }

        private static @Nullable TextRange rangeOf(int[] places, int start, int end) {
            if (start < 0 || end <= start || end * 4 > places.length) {
                return null;
            }
            var last = (end - 1) * 4;
            return new TextRange(places[start * 4], places[start * 4 + 1], places[last + 2], places[last + 3]);
        }
    }

    /**
     * Reads every key/value pair of the properties in the order they are written, a key written twice included,
     * each with the line it starts on.
     *
     * @see #load(Reader, BiConsumer)
     */
    public static List<Entry> readEntries(Reader input) throws IOException {
        return readEntries(input, false);
    }

    /**
     * Reads every key/value pair of the properties as {@link #readEntries(Reader)} does, and, when asked, where each
     * character of the keys and the values is written in the text.
     *
     * @param places whether to read the places of the characters, which costs four numbers for each of them
     */
    public static List<Entry> readEntries(Reader input, boolean places) throws IOException {
        var entries = new ArrayList<Entry>();
        new Parser(input, places, (key, value, line, read) -> entries.add(new Entry(key, value, line, read))).parse();
        return entries;
    }

    /**
     * Loads properties from the {@link InputStream} in UTF-8.
     *
     * @see #load(Reader, BiConsumer)
     */
    public static void load(InputStream input, BiConsumer<? super String, ? super String> result) throws IOException {
        load(new InputStreamReader(input, StandardCharsets.UTF_8), result);
    }

    /**
     * Loads properties from the {@link Path} in UTF-8.
     *
     * @see #load(Reader, BiConsumer)
     */
    public static void load(Path path, BiConsumer<? super String, ? super String> result) throws IOException {
        Objects.requireNonNull(path);
        try (var reader = Files.newBufferedReader(path)) {
            load(reader, result);
        }
    }

    /**
     * Loads properties from the {@link URL} in UTF-8.
     *
     * @see #load(InputStream, BiConsumer)
     */
    public static void load(URL url, BiConsumer<? super String, ? super String> result) throws IOException {
        Objects.requireNonNull(url);
        try (var reader = url.openStream()) {
            load(reader, result);
        }
    }

    /**
     * Writes properties in the output. Serialized properties are restored via {@link #load(Reader, BiConsumer)}
     * in the same order and in the same values, excepting where keys are {@literal null}.
     *
     * @param output a target where properties will be serialized
     * @param props  properties
     */
    public static <T extends Map.Entry<?, ?>> void store(Writer output, Iterable<T> props) throws IOException {

        for (Map.Entry<?, ?> entry : props) {
            var k = entry.getKey();
            var v = entry.getValue();
            if (k == null) {
                if (v != null) {
                    output.append('#').write(v.toString());
                }
            } else {
                var key = escape(k.toString()).replace(":", "\\:").replace("=", "\\=").replaceFirst("^#", "\\\\#");
                String value = escape(v.toString());
                output.append(key).append('=').write(value);
            }
            output.write('\n');
        }
        output.flush();
    }

    /**
     * Stores properties to the {@link OutputStream} in UTF-8.
     *
     * @see #store(Writer, Iterable)
     */
    public static <T extends Map.Entry<?, ?>> void store(OutputStream output, Iterable<T> props) throws IOException {
        store(new OutputStreamWriter(output, StandardCharsets.UTF_8), props);
    }

    /**
     * Stores properties to the file by {@link Path} in UTF-8.
     *
     * @see #store(OutputStream, Iterable)
     */
    public static <T extends Map.Entry<?, ?>> void store(Path path, Iterable<T> props) throws IOException {
        try (var writer = Files.newBufferedWriter(path)) {
            store(writer, props);
        }
    }

    private static String escape(String str) {
        return str
                .replace("\\", "\\\\")
                .replace("\f", "\\f")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    /** Receives a key/value pair, the line it starts on, and where its characters are written. */
    @FunctionalInterface
    private interface EntryConsumer {
        void accept(String key, @Nullable String value, int line, Places places);
    }

    /**
     * Reads properties char by char and sends every key/value pair to the target function as soon as its line ends.
     */
    @RequiredArgsConstructor
    private static final class Parser {

        private final Reader input;
        /** Whether the places of the characters are read. */
        private final boolean tracked;
        private final EntryConsumer result;
        private final StringBuilder str = new StringBuilder();
        private @Nullable String key;
        private boolean newLine = true;
        private boolean skipWhitespaces = true;
        private boolean skipLine;
        private boolean backSlash;
        private boolean ignoreLF;
        private int lastNonWhitespace;
        /** The line being read, and the one the pair being read starts on. */
        private int line = 1;
        private int entryLine = 1;
        /** The column of the character read next, and where the backslash of an escape being read stands. */
        private int column;
        private int escapeLine;
        private int escapeColumn;
        /** The places of the characters of the text being read, four numbers each, and those of the key read. */
        private int[] places = new int[64];
        private int placed;
        private int[] keyPlaces = new int[0];

        private void parse() throws IOException {
            for (var ch = input.read(); ch != -1; ch = input.read()) {
                read(ch);
            }
            endLine();
        }

        private void read(int ch) throws IOException {
            var crlf = ignoreLF && ch == '\n';
            ignoreLF = ch == '\r';
            if (crlf) {
                // The LF of a \r\n sequence, whose CR has already ended the line
                return;
            }
            var at = column;
            if (isLineBreak(ch)) {
                line++;
                column = 0;
            } else {
                column++;
            }
            if (!backSlash && isLineBreak(ch)) {
                endLine();
            } else if (newLine && (ch == '#' || ch == '!')) {
                skipLine = true;
                newLine = false;
            } else if (!skipLine && !(skipWhitespaces && Character.isWhitespace(ch))) {
                // Neither a comment, nor a whitespace in the beginning of a line or a value
                if (newLine) {
                    entryLine = line;
                }
                newLine = false;
                skipWhitespaces = false;
                readContent(ch, at);
            }
        }

        private void readContent(int ch, int at) throws IOException {
            if (backSlash) {
                backSlash = false;
                readEscaped(ch);
            } else if (ch == '\\') {
                backSlash = true;
                escapeLine = line;
                escapeColumn = at;
            } else if (key == null && (ch == ':' || ch == '=')) {
                // Key separator. It supports both - column and equal signs
                keyPlaces = takePlaces();
                key = takeText();
                skipWhitespaces = true;
            } else {
                str.append((char) ch);
                place(line, at);
                if (!Character.isWhitespace(ch)) {
                    lastNonWhitespace = str.length();
                }
            }
        }

        private void readEscaped(int ch) throws IOException {
            if (isLineBreak(ch)) {
                // The line continues on the next one, without its leading whitespaces
                skipWhitespaces = true;
            } else {
                str.append((char) unescape(ch));
                place(escapeLine, escapeColumn);
                lastNonWhitespace = str.length();
            }
        }

        /** Keeps where the character just added begins, and that it ends where the reading stands now. */
        private void place(int startLine, int startColumn) {
            if (!tracked) {
                return;
            }
            if (placed + 4 > places.length) {
                places = Arrays.copyOf(places, places.length * 2);
            }
            places[placed++] = startLine;
            places[placed++] = startColumn;
            places[placed++] = line;
            places[placed++] = column;
        }

        /** The places of the text taken next, its trailing whitespaces left out as they are left out of the text. */
        private int[] takePlaces() {
            var taken = tracked ? Arrays.copyOf(places, lastNonWhitespace * 4) : places;
            placed = 0;
            return taken;
        }

        private int unescape(int ch) throws IOException {
            return switch (ch) {
                case 't' -> '\t';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 'f' -> '\f';
                case 'u' -> readUnicode();
                // Any other escaped symbol stands for itself.
                default -> ch;
            };
        }

        private int readUnicode() throws IOException {
            var hex = new char[4];
            if (input.read(hex) < 4) {
                throw new EOFException("End of the data is reached unexpectedly");
            }
            column += 4;
            return Integer.parseInt(String.valueOf(hex), 16);
        }

        private void endLine() {
            var valuePlaces = takePlaces();
            var value = takeText();
            if (key != null) {
                result.accept(key, value, entryLine, placesOf(keyPlaces, valuePlaces));
                key = null;
            } else if (!value.isEmpty()) {
                // A line without a separator is a key alone.
                result.accept(value, null, entryLine, placesOf(valuePlaces, new int[0]));
            }
            newLine = true;
            skipWhitespaces = true;
            skipLine = false;
        }

        private String takeText() {
            var text = str.substring(0, lastNonWhitespace);
            lastNonWhitespace = 0;
            str.setLength(0);
            return text;
        }

        private Places placesOf(int[] key, int[] value) {
            return tracked ? new Places(key, value) : Places.NONE;
        }

        private static boolean isLineBreak(int ch) {
            return ch == '\r' || ch == '\n';
        }
    }
}
