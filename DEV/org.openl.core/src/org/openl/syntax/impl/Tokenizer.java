/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.exception.OpenLCompilationException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;

/**
 * The tokenizer class allows to break a source into tokens.
 *
 * @author snshor
 */
public class Tokenizer {

    private static final int EOF = -1;
    private static String TOKEN_TYPE = "token";

    private static Map<String, Tokenizer> tokenizers = new ConcurrentHashMap<>();

    private boolean[] delimitersTable = {};

    public Tokenizer(String delimiters) {
        makeTable(delimiters);
    }

    /**
     * Makes delimiters table. Delimiters table is array of boolean values where element index is delimiter character
     * code (char casted to integer) and value is boolean value that has <code>true</code> value if the character is
     * used as delimiter; <code>false</code> - otherwise.
     *
     * @param delimiters string of delimiters (each char is delimiter)
     */
    private void makeTable(String delimiters) {
        if (StringUtils.isEmpty(delimiters)) {
            return;
        }

        int min = 0;

        for (int i = 0; i < delimiters.length(); i++) {

            if (delimiters.charAt(i) > min) {
                min = delimiters.charAt(i);
            }
        }

        delimitersTable = new boolean[min + 1];

        for (int i = 0; i < delimiters.length(); i++) {
            delimitersTable[delimiters.charAt(i)] = true;
        }
    }

    /**
     * Checks that given character (his integer code) is delimiter.
     *
     * @param character character to check
     * @return <code>true</code> if character is delimiter; <code>false</code> - otherwise
     */
    private boolean isDelimiter(int character) {
        return character < delimitersTable.length && delimitersTable[character];
    }

    /**
     * Gets first token from source.
     *
     * @param source source
     * @return {@link IdentifierNode} object that represents first token in source
     * @throws OpenLCompilationException
     */
    private IdentifierNode firstToken(IOpenSourceCodeModule source) throws OpenLCompilationException {
        try {
            Reader reader = source.getCharacterStream();

            int startToken = 0;
            int position = -1;
            int character;
            StringBuilder buffer = null;

            do {
                character = reader.read();
                position += 1;

                if ((character == EOF || isDelimiter(character)) && buffer != null) {
                    String value = buffer.toString();
                    if (value.isEmpty()) {
                        buffer = null;
                    } else {
                        TextInterval location = LocationUtils.createTextInterval(startToken, position);

                        return new IdentifierNode(TOKEN_TYPE, location, value, source);
                    }
                } else {
                    if (character != EOF && !isDelimiter(character)) {
                        if (buffer == null) {
                            buffer = new StringBuilder();
                            startToken = position;
                        }

                        buffer.append((char) character);
                    }
                }

            } while (character != EOF);

            return new IdentifierNode(TOKEN_TYPE, LocationUtils.createTextInterval(0, 0), StringUtils.EMPTY, source);

        } catch (IOException e) {
            throw new OpenLCompilationException("Parsing error", e, null, source);
        }
    }

    public IdentifierNode[] parse(IOpenSourceCodeModule source,
            ILocation textLocation) throws OpenLCompilationException {
        List<IdentifierNode> nodes = new ArrayList<>();

        try {
            Reader reader = source.getCharacterStream();

            int startToken = 0;
            int position = -1;

            if (textLocation != null) {
                startToken = textLocation.getStart().getAbsolutePosition(null);
                position = textLocation.getStart().getAbsolutePosition(null) - 1;

                for (int i = 0; i < startToken; i++) {
                    reader.read();
                }
            }

            int character;
            StringBuilder buffer = null;
            boolean continueLooping;

            do {
                character = reader.read();
                position += 1;

                if (character == EOF || isDelimiter(character)) {
                    if (buffer != null) {
                        String value = buffer.toString().trim();
                        if (!value.isEmpty()) {
                            TextInterval location = LocationUtils.createTextInterval(startToken, position);
                            IdentifierNode node = new IdentifierNode(TOKEN_TYPE, location, value, source);
                            nodes.add(node);
                        }
                        buffer = null;
                    }
                } else {
                    if (!isDelimiter(character)) {
                        if (buffer == null) {
                            buffer = new StringBuilder();
                            startToken = position;
                        }
                        buffer.append((char) character);
                    }
                }

                if (textLocation != null) {
                    if (position < textLocation.getEnd().getAbsolutePosition(null)) {
                        continueLooping = character != EOF;
                    } else {
                        /* if end of token then save last token */
                        if (buffer != null) {
                            String value = buffer.toString().trim();
                            if (!value.isEmpty()) {
                                TextInterval location = LocationUtils.createTextInterval(startToken, position);
                                IdentifierNode node = new IdentifierNode(TOKEN_TYPE, location, value, source);
                                nodes.add(node);
                            }
                            buffer = null;
                        }

                        continueLooping = false;
                    }
                } else {
                    continueLooping = character != EOF;
                }
            } while (continueLooping);

        } catch (IOException e) {
            throw new OpenLCompilationException("Parsing error", e, null, source);
        }

        return nodes.toArray(new IdentifierNode[nodes.size()]);
    }

    public static IdentifierNode firstToken(IOpenSourceCodeModule source,
            String delimiter) throws OpenLCompilationException {
        return getTokenizer(delimiter).firstToken(source);
    }

    public static IdentifierNode[] tokenize(IOpenSourceCodeModule source,
            String delimiter) throws OpenLCompilationException {
        return getTokenizer(delimiter).parse(source, null);
    }

    private static Tokenizer getTokenizer(String delimeter) {

        Tokenizer tokenizer = tokenizers.get(delimeter);

        if (tokenizer == null) {
            tokenizer = new Tokenizer(delimeter);
            tokenizers.put(delimeter, tokenizer);
        }

        return tokenizer;
    }

    public static IdentifierNode[] tokenize(IOpenSourceCodeModule source,
            String delimiter,
            ILocation location) throws OpenLCompilationException {
        return getTokenizer(delimiter).parse(source, location);
    }

}
