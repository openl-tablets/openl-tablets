/*
 * Created on May 12, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.error.ASyntaxNodeError;
import org.openl.syntax.error.ISyntaxError;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 * 
 */
public class SyntaxError extends ASyntaxNodeError implements ISyntaxError {

    private static final long serialVersionUID = -6759448268772263778L;

    public SyntaxError(String message, Throwable cause, ILocation location, IOpenSourceCodeModule source) {
        super(message, cause, location, source);
    }

    public SyntaxError(String message, Throwable cause, ILocation location) {
        super(message, cause, location);
    }

    public SyntaxError(String message, Throwable cause, ISyntaxNode syntaxNode) {
        super(message, cause, syntaxNode);
    }

}
