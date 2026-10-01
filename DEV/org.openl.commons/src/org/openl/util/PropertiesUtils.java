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
        new Parser(input, result).parse();
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

    /**
     * Reads properties char by char and sends every key/value pair to the target function as soon as its line ends.
     */
    @RequiredArgsConstructor
    private static final class Parser {

        private final Reader input;
        private final BiConsumer<? super String, ? super String> result;
        private final StringBuilder str = new StringBuilder();
        private @Nullable String key;
        private boolean newLine = true;
        private boolean skipWhitespaces = true;
        private boolean skipLine;
        private boolean backSlash;
        private boolean ignoreLF;
        private int lastNonWhitespace;

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
            if (!backSlash && isLineBreak(ch)) {
                endLine();
            } else if (newLine && (ch == '#' || ch == '!')) {
                skipLine = true;
                newLine = false;
            } else if (!skipLine && !(skipWhitespaces && Character.isWhitespace(ch))) {
                // Neither a comment, nor a whitespace in the beginning of a line or a value
                newLine = false;
                skipWhitespaces = false;
                readContent(ch);
            }
        }

        private void readContent(int ch) throws IOException {
            if (backSlash) {
                backSlash = false;
                readEscaped(ch);
            } else if (ch == '\\') {
                backSlash = true;
            } else if (key == null && (ch == ':' || ch == '=')) {
                // Key separator. It supports both - column and equal signs
                key = takeText();
                skipWhitespaces = true;
            } else {
                str.append((char) ch);
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
                lastNonWhitespace = str.length();
            }
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
            return Integer.parseInt(String.valueOf(hex), 16);
        }

        private void endLine() {
            var value = takeText();
            if (key != null) {
                result.accept(key, value);
                key = null;
            } else if (!value.isEmpty()) {
                result.accept(value, null);
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

        private static boolean isLineBreak(int ch) {
            return ch == '\r' || ch == '\n';
        }
    }
}
