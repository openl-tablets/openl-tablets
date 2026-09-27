/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;

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
            return Set.of();
        }
        var ret = new HashSet<Integer>();
        for (var i = 0; i < x.length(); i++) {
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
            var reader = source.getCharacterStream();

            var position = -1;
            int character;
            var token = new TokenReader(source);
            do {
                var f = true;
                character = reader.read();
                position++;
                if (token.isEnd(character) && token.isStarted()) {
                    f = false;
                    var node = token.take(position);
                    if (node != null) {
                        return node;
                    }
                }
                if (f) {
                    token.append(character, position);
                }
            } while (character != EOF);

            return new IdentifierNode(TOKEN_TYPE, LocationUtils.createTextInterval(0, 0), StringUtils.EMPTY, source);

        } catch (IOException e) {
            throw new OpenLCompilationException("Parsing error", e, null, source);
        }
    }

    public IdentifierNode[] parse(IOpenSourceCodeModule source,
                                  ILocation textLocation) throws OpenLCompilationException {
        var nodes = new ArrayList<IdentifierNode>();

        try {
            var reader = source.getCharacterStream();

            var startToken = 0;
            var position = -1;

            if (textLocation != null) {
                startToken = textLocation.getStart().getAbsolutePosition(null);
                position = textLocation.getStart().getAbsolutePosition(null) - 1;

                skip(reader, startToken);
            }

            int character;
            var token = new TokenReader(source);
            boolean continueLooping;
            do {
                var f = true;
                character = reader.read();
                position++;
                if (token.isEnd(character)) {
                    f = false;
                    token.addTo(nodes, position);
                }
                if (f) {
                    token.append(character, position);
                }

                if (textLocation != null) {
                    if (position < textLocation.getEnd().getAbsolutePosition(null)) {
                        continueLooping = character != EOF;
                    } else {
                        /* if end of token then save last token */
                        token.addTo(nodes, position);

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

    /**
     * Skips the given number of characters; stops at the end of the stream.
     */
    private static void skip(Reader reader, int count) throws IOException {
        for (var i = 0; i < count; i++) {
            if (reader.read() < 0) {
                break;
            }
        }
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
        return tokenizers.computeIfAbsent(delimiter, Tokenizer::new);
    }

    public static IdentifierNode[] tokenize(IOpenSourceCodeModule source,
                                            String delimiter,
                                            ILocation location) throws OpenLCompilationException {
        return getTokenizer(delimiter).parse(source, location);
    }

    /**
     * The token being read from a source: its characters, the position of the first one, and whether an escaped part
     * of it is open.
     */
    @RequiredArgsConstructor
    private final class TokenReader {
        private final IOpenSourceCodeModule source;
        private StringBuilder buffer;
        private int startToken;
        private boolean escaped;

        /**
         * Tells whether the character ends the token: the end of the source or a delimiter outside an escaped part.
         * An escape character opens or closes an escaped part instead.
         */
        private boolean isEnd(int character) {
            if (!escaped && isEscapeBegin(character)) {
                escaped = true;
                return false;
            }
            if (escaped && isEscapeEnd(character)) {
                escaped = false;
                return false;
            }
            return character == EOF || !escaped && isDelimiter(character);
        }

        private boolean isStarted() {
            return buffer != null;
        }

        private void append(int character, int position) {
            if (buffer == null) {
                buffer = new StringBuilder();
                startToken = position;
            }

            buffer.append((char) character);
        }

        /**
         * Takes the token read so far, so that the next character starts a new one.
         *
         * @return the token, or {@code null} when it is blank
         */
        private IdentifierNode take(int position) {
            var value = buffer.toString().trim();
            buffer = null;
            if (value.isEmpty()) {
                return null;
            }
            TextInterval location = LocationUtils.createTextInterval(startToken, position);
            return new IdentifierNode(TOKEN_TYPE, location, value, source);
        }

        /**
         * Adds the token read so far unless it is blank.
         */
        private void addTo(List<IdentifierNode> nodes, int position) {
            if (buffer != null) {
                var node = take(position);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }
    }
}
