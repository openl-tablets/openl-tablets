package org.openl.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * An utility for working with properties files. Parsing, storing.
 *
 * @author Yury Molchan
 */
public class PropertiesUtils {

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

    public static void load(InputStream input, BiConsumer<? super String, ? super String> result) throws IOException {
        load(new InputStreamReader(input, StandardCharsets.UTF_8), result);
    }
}
