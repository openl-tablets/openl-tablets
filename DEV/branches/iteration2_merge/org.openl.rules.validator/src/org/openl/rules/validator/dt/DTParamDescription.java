package org.openl.rules.validator.dt;

import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

public class DTParamDescription 
{
	public DTParamDescription(IParameterDeclaration parameterDeclaration,
			IOpenClass newType) 
	{
		this.originalDeclaration = parameterDeclaration;
		this.newType = newType;
		domain = originalDeclaration.getType().getDomain();
	}
	IParameterDeclaration originalDeclaration;
	IDomain<?> domain;
	IOpenClass newType;

	public IOpenClass getNewType() {
		return newType;
	}

	public IParameterDeclaration getOriginalDeclaration() {
		return originalDeclaration;
	}

	public IDomain<?> getDomain() {
		return domain;
	}
}
