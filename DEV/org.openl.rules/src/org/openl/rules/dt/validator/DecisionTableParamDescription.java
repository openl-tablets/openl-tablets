package org.openl.rules.dt.validator;

import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

public class DecisionTableParamDescription {

    private final IParameterDeclaration parameterDeclaration;
    private final IDomain<?> domain;
    private final IOpenClass newType;

    public DecisionTableParamDescription(IParameterDeclaration parameterDeclaration, IOpenClass newType) {
        this.parameterDeclaration = parameterDeclaration;
        this.newType = newType;
        this.domain = this.parameterDeclaration.getType().getDomain();
    }

    public IDomain<?> getDomain() {
        return domain;
    }

    public IOpenClass getNewType() {
        return newType;
    }

    public IParameterDeclaration getParameterDeclaration() {
        return parameterDeclaration;
    }
}
