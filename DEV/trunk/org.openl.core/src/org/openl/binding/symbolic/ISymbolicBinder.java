package org.openl.binding.symbolic;

import org.openl.syntax.ISyntaxNode;

public interface ISymbolicBinder {

    public void bindSymbolic(ISyntaxNode node, ISymbolicBindingContext cxt);

}
