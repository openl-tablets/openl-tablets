package org.openl.rules.liveexcel.usermodel;

import org.openl.rules.liveexcel.EvaluationContext;
import org.openl.rules.liveexcel.ServiceModelAPI;

public class ContextFactory {
    
    private static ServiceModelAPI EMPTY_API = new EmptyServiceModelAPI(); 
    
    public static EvaluationContext getEvaluationContext(String projectName) {
        if (projectName == null) {
            return new EvaluationContext(EMPTY_API);
        } else {
            return new EvaluationContext(new ServiceModelAPI(projectName));
        }
    }

}
