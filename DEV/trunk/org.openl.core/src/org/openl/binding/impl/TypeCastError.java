/**
 * Created Jul 21, 2007
 */
package org.openl.binding.impl;

import org.openl.binding.error.BoundError;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class TypeCastError extends BoundError {

    private static final long serialVersionUID = 5570752529258476343L;

    private IOpenClass from;
    private IOpenClass to;

    public TypeCastError(ISyntaxNode node, IOpenClass from, IOpenClass to) {
        super("Can not convert from " + from.getName() + " to " + to.getName(), node);

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
