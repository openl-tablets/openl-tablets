package org.openl.rules.dt;

import org.openl.types.IParameterDeclaration;

public interface IDecisionTableParameterInfo {

    IParameterDeclaration getParameterDeclaration();

    String getPresentation();

    Object getValue(int row);

}
