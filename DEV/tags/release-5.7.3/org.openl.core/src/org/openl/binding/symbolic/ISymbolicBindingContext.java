package org.openl.binding.symbolic;

import org.openl.syntax.ISyntaxNode;

public interface ISymbolicBindingContext {
    public void addSymbolicMethod(String methodName, String typeName, String[] symbolicParams, ISyntaxNode node);

    public void addSymbolicType(String typeName, ISyntaxNode sourceNode);

    public void addSymbolicVar(String varName, String typeName, ISyntaxNode sourceNode);

}
