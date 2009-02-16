package org.openl.rules.tbasic.runtime;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

class AlgorithmErrorHelper {

    public static Object processError(Throwable error, TBasicContextHolderEnv environment) {
     // TODO discover which exception contains exception
        IOpenClass algorithmType = environment.getTbasicTarget().getType();
        IOpenMethod errorMethod = algorithmType.getMethod("ON ERROR", new IOpenClass[]{});
        
        if (errorMethod != null){
            IOpenField errorField = algorithmType.getField("ERROR");
            if (errorField != null){
                // populate error messages
                errorField.set(environment.getTbasicTarget(), error, environment);
            }
            return errorMethod.invoke(environment.getTbasicTarget(), null, environment);
        }
        
        throw new RuntimeException(String.format("Execution of algorithm failed: %s", error.getMessage()), error);
    }

}
