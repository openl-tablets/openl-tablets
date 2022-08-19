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

        StringBuilder str = new StringBuilder();
        String key = null;

        boolean newLine = true;
        boolean skipWhitespaces = true;
        boolean skipLine = false;
        boolean backSlash = false;
        boolean ignoreLF = false;

        int ch;
        while ((ch = input.read()) != -1) {

            if (ignoreLF && ch == '\n') {
                // Ignore in \r\n sequence
                ignoreLF = false;
                continue;
            }
            ignoreLF = ch == '\r';

            if (!backSlash && (ch == '\r' || ch == '\n')) {
                newLine = true;
                skipWhitespaces = true;
                if (key != null) {
                    result.accept(key, str.toString());
                    key = null;
                }
                str.setLength(0);
                //Do new line;
                continue;
            }

            if (newLine && (ch == '#' || ch == '!')) {
                // skip comments
                skipLine = true;
                newLine = false;
                continue;
            }

            if (skipWhitespaces && Character.isWhitespace(ch)) {
                // Skip whitespaces in the beginning
                continue;
            }
            skipWhitespaces = false;

            if (skipLine && !newLine) {
                continue;
            }
            skipLine = false;
            newLine = false;

            if (backSlash) {
                // escaped symbols via backslash
                backSlash = false;
                switch (ch) {
                    case '\n':
                    case '\r':
                        continue;

                    case 't':
                        ch = '\t';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'u': {
                        char[] hex = new char[4];
                        ch = input.read(hex);
                        if (ch < 4) {
                            throw new EOFException("End of the data is reached unexpectedly");
                        }
                        ch = Integer.parseInt(String.valueOf(hex), 16);
                        break;

                    }
                }
                str.append((char) ch);
                continue;
            }

            if (ch == '\\') {
                backSlash = true;
                continue;
            }

            if (key == null && (ch == ':' || ch == '=')) {
                // Key separator. It supports both - column and equal signs
                skipWhitespaces = true;
                key = str.toString().trim();
                str.setLength(0);
                continue;
            }
            str.append((char) ch);
        }
        if (key != null) {
            // EOF
            result.accept(key, str.toString());
        }
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
            Object k = entry.getKey();
            Object v = entry.getValue();
            if (k == null) {
                if (v != null) {
                    output.append('#').write(v.toString());
                }
            } else {
                String key = escape(k.toString()).replace(":", "\\:").replace("=", "\\=").replaceFirst("^#", "\\\\#");
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
}
