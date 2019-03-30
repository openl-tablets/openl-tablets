/*
 * Created on Jun 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.IOpenParser;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.grammar.IGrammar;
import org.openl.syntax.grammar.IGrammarFactory;

/**
 * Class provides default abstract implementation of parser based on {@link IGrammarFactory}.
 * 
 * @author snshor
 * 
 */
public abstract class AParser implements IOpenParser {

    private static final String INTEGER_RANGE_PARSING_TYPE = "range.literal";
    private static final String FLOAT_RANGE_PARSING_TYPE = "range.literal.real";

    protected abstract IGrammarFactory getGrammarFactory();

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsMethodBody(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parseAsMethod(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parseAsMethodHeader(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parseAsModule(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsType(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parseAsType(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsFloatRange(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parse(source.getCharacterStream(), FLOAT_RANGE_PARSING_TYPE);

        return makeParsedCode(grammar, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsIntegerRange(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parse(source.getCharacterStream(), INTEGER_RANGE_PARSING_TYPE);

        return makeParsedCode(grammar, source);
    }

    /**
     * Creates {@link ParsedCode} object.
     * 
     * @param grammar grammar
     * @param source source
     * @return new instance of {@link ParsedCode}
     */
    private ParsedCode makeParsedCode(IGrammar grammar, IOpenSourceCodeModule source) {

        ISyntaxNode node = grammar.getTopNode();

        return new ParsedCode(node, source, grammar.getErrors(), null);
    }

}
