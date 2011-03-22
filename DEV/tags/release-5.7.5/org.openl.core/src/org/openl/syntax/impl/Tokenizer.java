/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.exception.OpenLCompilationException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.AbsolutePosition;
import org.openl.util.text.TextInterval;

/**
 * The tokenizer class allows to break a source into tokens.
 * 
 * @author snshor
 */
public class Tokenizer {

    private static final int EOF = -1;
    private static String TOKEN_TYPE = "token";

    private static Map<String, Tokenizer> tokenizers = new HashMap<String, Tokenizer>();

    private boolean[] delimitersTable = {};

    public Tokenizer(String delimiters) {
        makeTable(delimiters);
    }

    /**
     * Makes delimiters table. Delimiters table is array of boolean values where
     * element index is delimiter character code (char casted to integer) and
     * value is boolean value that has <code>true</code> value if the character
     * is used as delimiter; <code>false</code> - otherwise.
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
     * @return <code>true</code> if character is delimiter; <code>false</code> -
     *         otherwise
     */
    private boolean isDelimiter(int character) {
        return character < delimitersTable.length && delimitersTable[character];
    }

    /**
     * Gets first token from source.
     * 
     * @param source source
     * @return {@link IdentifierNode} object that represents first token in
     *         source
     * @throws OpenLCompilationException
     */
    private IdentifierNode firstToken(IOpenSourceCodeModule source) throws OpenLCompilationException {

        try {
            Reader reader = source.getCharacterStream();

            int startToken = 0;
            int position = 0;
            int character;
            StringBuffer buffer = null;

            do {
                character = reader.read();
                position += 1;

                if ((character == EOF || isDelimiter(character)) && buffer != null) {

                    TextInterval location = new TextInterval(new AbsolutePosition(startToken),
                        new AbsolutePosition(position));

                    return new IdentifierNode(TOKEN_TYPE, location, buffer.toString().trim(), source);
                } else {
                    if (buffer == null) {
                        buffer = new StringBuffer();
                        startToken = position;
                    }

                    buffer.append((char) character);
                }

            } while (character != EOF);

            return new IdentifierNode(TOKEN_TYPE,
                new TextInterval(new AbsolutePosition(0), new AbsolutePosition(0)),
                StringUtils.EMPTY,
                source);

        } catch (IOException e) {
            throw new OpenLCompilationException("Parsing error", e, null, source);
        }
    }

    public IdentifierNode[] parse(IOpenSourceCodeModule source) throws OpenLCompilationException {

        List<IdentifierNode> nodes = new ArrayList<IdentifierNode>();

        try {
            Reader reader = source.getCharacterStream();

            int startToken = 0;
            int position = -1;
            int character;
            StringBuffer buffer = null;

            do {
                character = reader.read();
                position += 1;

                if (character == EOF || isDelimiter(character)) {

                    if (buffer != null) {

                        TextInterval location = new TextInterval(new AbsolutePosition(startToken),
                            new AbsolutePosition(position));

                        IdentifierNode node = new IdentifierNode(TOKEN_TYPE, location, buffer.toString().trim(), source);
                        buffer = null;

                        nodes.add(node);
                    }
                } else {
                    if (buffer == null) {
                        buffer = new StringBuffer();
                        startToken = position;
                    }

                    buffer.append((char) character);
                }

            } while (character != EOF);

        } catch (IOException e) {
            throw new OpenLCompilationException("Parsing error", e, null, source);
        }

        return nodes.toArray(new IdentifierNode[nodes.size()]);
    }

    public static IdentifierNode firstToken(IOpenSourceCodeModule source, String delimiter) throws OpenLCompilationException {
        return getTokenizer(delimiter).firstToken(source);
    }

    public static IdentifierNode[] tokenize(IOpenSourceCodeModule source, String delimiter) throws OpenLCompilationException {
        return getTokenizer(delimiter).parse(source);
    }

    private static synchronized Tokenizer getTokenizer(String delimeter) {

        Tokenizer tokenizer = tokenizers.get(delimeter);

        if (tokenizer == null) {
            tokenizer = new Tokenizer(delimeter);
            tokenizers.put(delimeter, tokenizer);
        }

        return tokenizer;
    }

}
