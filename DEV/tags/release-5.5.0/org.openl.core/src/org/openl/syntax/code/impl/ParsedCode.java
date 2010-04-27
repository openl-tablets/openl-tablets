/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.code.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * @author snshor
 * 
 */
public class ParsedCode implements IParsedCode {

    private ISyntaxNode topNode;
    private SyntaxNodeException[] syntaxErrors;
    private IOpenSourceCodeModule source;

    public ParsedCode(ISyntaxNode topnode, IOpenSourceCodeModule source, SyntaxNodeException[] syntaxErrors) {

        this.topNode = topnode;
        this.syntaxErrors = syntaxErrors;
        this.source = source;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.syntax.IParsedCode#getError()
     */
    public SyntaxNodeException[] getErrors() {

        return syntaxErrors;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.syntax.IParsedCode#getSource()
     */
    public IOpenSourceCodeModule getSource() {

        return source;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.syntax.IParsedCode#getTopNode()
     */
    public ISyntaxNode getTopNode() {

        return topNode;
    }

}
