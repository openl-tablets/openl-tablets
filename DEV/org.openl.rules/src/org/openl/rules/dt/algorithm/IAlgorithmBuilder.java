package org.openl.rules.dt.algorithm;

import org.openl.binding.IBindingContext;
import org.openl.syntax.exception.SyntaxNodeException;

public interface IAlgorithmBuilder {

    IDecisionTableAlgorithm prepareAndBuildAlgorithm(IBindingContext bindingContext) throws SyntaxNodeException;

}
