/*
 * Created on May 9, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.code.impl;

import java.util.Map;

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
    
    private Map<String, Object> params;

    public ParsedCode(ISyntaxNode topnode, IOpenSourceCodeModule source, SyntaxNodeException[] syntaxErrors) {
        this.topNode = topnode;
        this.syntaxErrors = syntaxErrors;
        this.source = source;
    }

    public SyntaxNodeException[] getErrors() {
        return syntaxErrors;
    }

    public IOpenSourceCodeModule getSource() {
        return source;
    }

    public ISyntaxNode getTopNode() {
        return topNode;
    }

    public Map<String, Object> getExternalParams() {
        return params;
    }

    public void setExternalParams(Map<String, Object> params) {
        this.params = params;
    }
    
}
