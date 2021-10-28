/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public final class Tokenizer {

    private static final int EOF = -1;
    private static final String TOKEN_TYPE = "token";

    private static final Map<String, Tokenizer> tokenizers = new ConcurrentHashMap<>();

    private final Set<Integer> delimitersTable;

    private Tokenizer(String delimiters) {
        delimitersTable = makeTable(delimiters);
    }

    private Set<Integer> makeTable(String x) {
        if (StringUtils.isEmpty(x)) {
            return Collections.emptySet();
        }
        Set<Integer> ret = new HashSet<>();
        for (int i = 0; i < x.length(); i++) {
            ret.add((int) x.charAt(i));
        }
        return ret;
    }

    /**
     * Checks that given character (his integer code) is delimiter.
     *
     * @param character character to check
     * @return <code>true</code> if character is delimiter; <code>false</code> - otherwise
     */
    private boolean isDelimiter(int character) {
        return delimitersTable.contains(character);
    }

    private boolean isEscapeBegin(int character) {
        return character == '`';
    }

    private boolean isEscapeEnd(int character) {
        return character == '`';
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
            boolean escaped = false;
            do {
                boolean f = true;
                character = reader.read();
                position++;
                if (!escaped && isEscapeBegin(character)) {
                    escaped = true;
                } else if (escaped && isEscapeEnd(character)) {
                    escaped = false;
                } else if ((character == EOF || !escaped && isDelimiter(character)) && buffer != null) {
                    f = false;
                    String value = buffer.toString().trim();
                    if (value.isEmpty()) {
                        buffer = null;
                    } else {
                        TextInterval location = LocationUtils.createTextInterval(startToken, position);
                        return new IdentifierNode(TOKEN_TYPE, location, value, source);
                    }
                }
                if (f) {
                    if (buffer == null) {
                        buffer = new StringBuilder();
                        startToken = position;
                    }

                    buffer.append((char) character);
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
            boolean escaped = false;
            do {
                boolean f = true;
                character = reader.read();
                position++;
                if (!escaped && isEscapeBegin(character)) {
                    escaped = true;
                } else if (escaped && isEscapeEnd(character)) {
                    escaped = false;
                } else if (character == EOF || !escaped && isDelimiter(character)) {
                    f = false;
                    if (buffer != null) {
                        String value = buffer.toString().trim();
                        if (!value.isEmpty()) {
                            TextInterval location = LocationUtils.createTextInterval(startToken, position);
                            IdentifierNode node = new IdentifierNode(TOKEN_TYPE, location, value, source);
                            nodes.add(node);
                        }
                        buffer = null;
                    }
                }
                if (f) {
                    if (buffer == null) {
                        buffer = new StringBuilder();
                        startToken = position;
                    }

                    buffer.append((char) character);
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

        return nodes.toArray(new IdentifierNode[0]);
    }

    public static IdentifierNode firstToken(IOpenSourceCodeModule source,
            String delimiter) throws OpenLCompilationException {
        return getTokenizer(delimiter).firstToken(source);
    }

    public static IdentifierNode[] tokenize(IOpenSourceCodeModule source,
            String delimiter) throws OpenLCompilationException {
        return getTokenizer(delimiter).parse(source, null);
    }

    private static Tokenizer getTokenizer(String delimiter) {

        Tokenizer tokenizer = tokenizers.get(delimiter);

        if (tokenizer == null) {
            tokenizer = new Tokenizer(delimiter);
            tokenizers.put(delimiter, tokenizer);
        }

        return tokenizer;
    }

    public static IdentifierNode[] tokenize(IOpenSourceCodeModule source,
            String delimiter,
            ILocation location) throws OpenLCompilationException {
        return getTokenizer(delimiter).parse(source, location);
    }

}
