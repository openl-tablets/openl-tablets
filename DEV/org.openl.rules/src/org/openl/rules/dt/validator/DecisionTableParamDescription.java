package org.openl.rules.dt.validator;

import lombok.Getter;

import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

public class DecisionTableParamDescription {

    @Getter
    private final IParameterDeclaration parameterDeclaration;
    @Getter
    private final IDomain<?> domain;
    @Getter
    private final IOpenClass newType;

    public DecisionTableParamDescription(IParameterDeclaration parameterDeclaration, IOpenClass newType) {
        this.parameterDeclaration = parameterDeclaration;
        this.newType = newType;
        this.domain = this.parameterDeclaration.getType().getDomain();
    }
}
