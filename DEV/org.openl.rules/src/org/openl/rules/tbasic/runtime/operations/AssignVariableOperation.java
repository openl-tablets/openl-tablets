package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * Created by dl on 9/10/14.
 */
public class AssignVariableOperation extends RuntimeOperation {

    private final String elementName;

    public AssignVariableOperation(String label, String elementName) {
        this.elementName = elementName;
    }

    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        environment.getTbasicTarget().setFieldValue(elementName, param);
        return new Result(ReturnType.NEXT, param);
    }
}
