/*
 * Created on Jun 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBoundError;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class BoundError extends SyntaxError implements IBoundError {

    /**
     *
     */
    private static final long serialVersionUID = 5982280103016729377L;

    /**
     * @param location
     * @param msg
     * @param t
     */
    public BoundError(ILocation location, String msg, Throwable t, IOpenSourceCodeModule module) {
        super(location, msg, t, module);
    }

    public BoundError(ISyntaxNode node, String msg) {
        super(node, msg, null);
    }

    /**
     * @param node
     * @param msg
     * @param t
     */
    public BoundError(ISyntaxNode node, String msg, Throwable t) {
        super(node, msg, t);
    }

    public BoundError(String msg, IOpenSourceCodeModule module) {
        super(null, msg, null, module);
    }

    public BoundError(Throwable t, IOpenSourceCodeModule module) {
        super(null, null, t, module);
    }

}
