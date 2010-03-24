/*
 * Created on Jun 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.error;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.error.ASyntaxNodeError;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 * 
 */
public class BoundError extends ASyntaxNodeError implements IBoundError {

    private static final long serialVersionUID = 5982280103016729377L;

    public BoundError(String message, Throwable cause, ILocation location, IOpenSourceCodeModule source) {
        super(message, cause, location, source);
    }

    public BoundError(String message, Throwable cause, ISyntaxNode syntaxNode) {
        super(message, cause, syntaxNode);
    }

    public BoundError(String message, IOpenSourceCodeModule source) {
        super(message, null, null, source);
    }

    public BoundError(String message, ISyntaxNode syntaxNode) {
        super(message, null, syntaxNode);
    }

    public BoundError(Throwable cause, IOpenSourceCodeModule source) {
        super(null, cause, null, source);
    }

}
