package org.openl.syntax.error;

import org.openl.error.IOpenLError;
import org.openl.syntax.ISyntaxNode;

/**
 * Error that occurs during OpenL (parsing, binding) operations with {@link ISyntaxNode}.
 */
public interface ISyntaxNodeError extends IOpenLError {

    /**
     * Gets syntax node where error was appeared.
     * 
     * @return {@link ISyntaxNode} instance
     */
    ISyntaxNode getSyntaxNode();

    // to improve navigation
    ISyntaxNode getTopLevelSyntaxNode();

    void setTopLevelSyntaxNode(ISyntaxNode topLevelNode);

}
