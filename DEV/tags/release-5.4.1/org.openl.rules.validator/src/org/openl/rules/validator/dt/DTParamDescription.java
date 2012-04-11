package org.openl.rules.validator.dt;

import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

public class DTParamDescription {
    
    private IParameterDeclaration originalDeclaration;

    private IDomain<?> domain;
    
    private IOpenClass newType;
    
    public DTParamDescription(IParameterDeclaration parameterDeclaration, IOpenClass newType) {
        originalDeclaration = parameterDeclaration;
        this.newType = newType;
        domain = originalDeclaration.getType().getDomain();
    }

    public IDomain<?> getDomain() {
        return domain;
    }

    public IOpenClass getNewType() {
        return newType;
    }

    public IParameterDeclaration getOriginalDeclaration() {
        return originalDeclaration;
    }
}
