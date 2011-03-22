package org.openl.rules.validation.properties.dimentional;

import org.openl.types.IOpenClass;

public interface IDecisionTableReturnColumn extends IDecisionTableColumn {
    
    String paramsThroughComma();
    
    IOpenClass getReturnType();

}
