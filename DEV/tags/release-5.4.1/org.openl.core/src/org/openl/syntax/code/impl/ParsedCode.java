/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.code.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;

/**
 * @author snshor
 * 
 */
public class ParsedCode implements IParsedCode {

    private ISyntaxNode topnode;
    private ISyntaxError[] syntaxErrors;
    private IOpenSourceCodeModule source;

    public ParsedCode(ISyntaxNode topnode, IOpenSourceCodeModule source, ISyntaxError[] syntaxErrors) {

        this.topnode = topnode;
        this.syntaxErrors = syntaxErrors;
        this.source = source;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.syntax.IParsedCode#getError()
     */
    public ISyntaxError[] getErrors() {

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

        return topnode;
    }

}
