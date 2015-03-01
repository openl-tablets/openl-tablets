package org.openl.rules.dtx;

import org.openl.types.IParameterDeclaration;

public interface IDecisionTableParameterInfo {

	IParameterDeclaration getParameterDeclaration();

	String getPresentation();

	Object getValue(int row);

}
