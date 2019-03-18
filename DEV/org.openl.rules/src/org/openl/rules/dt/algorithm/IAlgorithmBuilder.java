package org.openl.rules.dt.algorithm;

import org.openl.binding.IBindingContext;

public interface IAlgorithmBuilder {

    IDecisionTableAlgorithm prepareAndBuildAlgorithm(IBindingContext bindingContext) throws Exception;

}
