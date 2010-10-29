package org.openl.binding.symbolic;

import org.openl.syntax.ISyntaxNode;

public interface ISymbolicBindingContext {

    void addSymbolicMethod(String methodName, String typeName, String[] symbolicParams, ISyntaxNode node);

    void addSymbolicType(String typeName, ISyntaxNode sourceNode);

    void addSymbolicVar(String varName, String typeName, ISyntaxNode sourceNode);

}
