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

    protected abstract IGrammarFactory getGrammarFactory();

    ParsedCode makeParsedCode(IGrammar g, IOpenSourceCodeModule src) {
        ISyntaxNode node = g.getTopNode();
        return new ParsedCode(node, src, g.getErrors());
    }

    public IParsedCode parse(IOpenSourceCodeModule src, String parseType) {
        IGrammar g = getGrammarFactory().getGrammar();
        g.setModule(src);
        g.parse(src.getCharacterStream(), parseType);

        return makeParsedCode(g, src);

    }

    public IParsedCode parseAsMethodBody(IOpenSourceCodeModule src) {

        IGrammar g = getGrammarFactory().getGrammar();
        g.setModule(src);
        g.parseAsMethod(src.getCharacterStream());

        return makeParsedCode(g, src);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenParser#parse(java.lang.String)
     */
    // public IParsedCode parseAsModule(String code)
    // {
    // return parseAsModule(new SourceCodeModule(code, null));
    // }
    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenParser#parse(java.lang.String)
     */
    // public IParsedCode parseAsMethod(String code)
    // {
    // return parseAsMethod(new SourceCodeModule(code, null));
    // }
    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenParser#parseAsMethodHeader(org.openl.IOpenSourceCodeModule)
     */
    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule src) {
        IGrammar g = getGrammarFactory().getGrammar();
        g.setModule(src);
        g.parseAsMethodHeader(src.getCharacterStream());

        return makeParsedCode(g, src);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenParser#parse(org.openl.IOpenModule)
     */
    public IParsedCode parseAsModule(IOpenSourceCodeModule src) {

        IGrammar g = getGrammarFactory().getGrammar();
        g.setModule(src);
        g.parseAsModule(src.getCharacterStream());

        return makeParsedCode(g, src);
    }

    /**
     *
     */

    public IParsedCode parseAsType(IOpenSourceCodeModule src) {
        IGrammar g = getGrammarFactory().getGrammar();
        g.setModule(src);
        g.parseAsType(src.getCharacterStream());

        return makeParsedCode(g, src);
    }

}
