package org.openl.rules.lang.xls;

import org.openl.IOpenParser;
import org.openl.j.BExGrammarWithParsingHelp;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.grammar.IGrammar;

/**
 * Default implementation of {@link IOpenParser}.
 *
 * @author snshor
 */
public class Parser implements IOpenParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsMethodBody(IOpenSourceCodeModule source) {

        IGrammar grammar = createGrammar();
        grammar.setModule(source);
        grammar.parseAsMethod(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsMethodHeader(IOpenSourceCodeModule source) {

        IGrammar grammar = createGrammar();
        grammar.setModule(source);
        grammar.parseAsMethodHeader(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {

        IGrammar grammar = createGrammar();
        grammar.setModule(source);
        grammar.parseAsModule(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IParsedCode parseAsType(IOpenSourceCodeModule source) {

        IGrammar grammar = createGrammar();
        grammar.setModule(source);
        grammar.parseAsType(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }

    /**
     * Creates {@link ParsedCode} object.
     *
     * @param grammar grammar
     * @param source  source
     * @return new instance of {@link ParsedCode}
     */
    private ParsedCode makeParsedCode(IGrammar grammar, IOpenSourceCodeModule source) {

        ISyntaxNode node = grammar.getTopNode();

        SyntaxNodeException error = grammar.getError();
        SyntaxNodeException[] nonNullError = error == null ? SyntaxNodeException.EMPTY_ARRAY
                : new SyntaxNodeException[]{error};
        return new ParsedCode(node, source, nonNullError, null);
    }

    @Override
    public IParsedCode parseAsParameterDeclaration(IOpenSourceCodeModule source) {
        IGrammar grammar = createGrammar();
        grammar.setModule(source);
        grammar.parseAsParamDeclaration(source.getCharacterStream());

        return makeParsedCode(grammar, source);
    }


    private IGrammar createGrammar() {
        return new BExGrammarWithParsingHelp();
    }
}
