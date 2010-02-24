/*
 * Created on Jun 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.IOpenParser;
import org.openl.IOpenSourceCodeModule;
import org.openl.syntax.IGrammar;
import org.openl.syntax.IGrammarFactory;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public abstract class AParser implements IOpenParser {

    private static final String INTEGER_RANGE_PARSING_TYPE = "range.literal";
    private static final String FLOAT_RANGE_PARSING_TYPE = "range.literal.real";

    protected abstract IGrammarFactory getGrammarFactory();

    protected IParsedCode parse(IOpenSourceCodeModule source, String parsingType) {
        
        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parse(source.getCharacterStream(), parsingType);

        return makeParsedCode(grammar, source);

    }

    public IParsedCode parseAsMethodBody(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parseAsMethod(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenParser#parseAsMethodHeader(org.openl.IOpenSourceCodeModule)
     */
    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parseAsMethodHeader(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenParser#parse(org.openl.IOpenModule)
     */
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parseAsModule(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    public IParsedCode parseAsType(IOpenSourceCodeModule source) {
        
        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parseAsType(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }
    
    public IParsedCode parseAsFloatRange(IOpenSourceCodeModule source) {

        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parse(source.getCharacterStream(), FLOAT_RANGE_PARSING_TYPE);

        return makeParsedCode(grammar, source);
    }

    public IParsedCode parseAsIntegerRange(IOpenSourceCodeModule source) {
        
        IGrammar grammar = getGrammarFactory().getGrammar();
        grammar.setModule(source);
        grammar.parse(source.getCharacterStream(), INTEGER_RANGE_PARSING_TYPE);

        return makeParsedCode(grammar, source);
    }

    private ParsedCode makeParsedCode(IGrammar grammar, IOpenSourceCodeModule source) {
        
        ISyntaxNode node = grammar.getTopNode();

        return new ParsedCode(node, source, grammar.getErrors());
    }

}
