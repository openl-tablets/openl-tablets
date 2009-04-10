/**
 * Created Jul 21, 2007
 */
package org.openl.binding.impl;

import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class TypeCastError extends BoundError {
    private static final long serialVersionUID = 5570752529258476343L;
    IOpenClass from;

    IOpenClass to;

    /**
     * @param node
     * @param msg
     */
    public TypeCastError(ISyntaxNode node, IOpenClass from, IOpenClass to) {
        super(node, "Can not convert from " + from.getName() + " to " + to.getName());
        this.from = from;
        this.to = to;
    }

    public IOpenClass getFrom() {
        return from;
    }

    public IOpenClass getTo() {
        return to;
    }

}
