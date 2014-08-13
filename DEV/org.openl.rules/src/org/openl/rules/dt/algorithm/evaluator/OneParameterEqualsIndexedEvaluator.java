package org.openl.rules.dt.algorithm.evaluator;

import org.openl.rules.dt.element.ICondition;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IParameterDeclaration;

public class OneParameterEqualsIndexedEvaluator extends EqualsIndexedEvaluator {
    IParameterDeclaration parameterDeclaration;

    public OneParameterEqualsIndexedEvaluator(IParameterDeclaration parameterDeclaration) {
        if (parameterDeclaration == null) {
            throw new IllegalArgumentException("parameterDeclaration");
        }
        this.parameterDeclaration = parameterDeclaration;
    }

    @Override
    public String getOptimizedSourceCode() {
        return parameterDeclaration.getName();
    }

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(ICondition condition) {
        return condition.getSourceCodeModule();
    }
}
