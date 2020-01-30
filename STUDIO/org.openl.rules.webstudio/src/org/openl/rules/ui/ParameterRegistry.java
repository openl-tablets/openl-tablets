package org.openl.rules.ui;

import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class ParameterRegistry extends ObjectRegistry<ParameterWithValueDeclaration> {
    private static ParameterRegistry getCurrent(String requestId) {
        return (ParameterRegistry) WebStudioUtils.getExternalContext().getSessionMap()
            .get(Constants.SESSION_PARAM_PARAMETERS + requestId);
    }

    private static ParameterRegistry getCurrentOrCreate(String requestId) {
        ParameterRegistry parameterRegistry = getCurrent(requestId);
        if (parameterRegistry == null) {
            parameterRegistry = new ParameterRegistry();
            WebStudioUtils.getExternalContext().getSessionMap()
                .put(Constants.SESSION_PARAM_PARAMETERS + requestId, parameterRegistry);
        }
        return parameterRegistry;
    }

    public static int getUniqueId(String requestId, ParameterWithValueDeclaration value) {
        return getCurrentOrCreate(requestId).putIfAbsent(value);
    }

    public static ParameterWithValueDeclaration getParameter(String requestId, String rootID) {
        ParameterRegistry parameterRegistry = getCurrent(requestId);
        return parameterRegistry == null ? null : parameterRegistry.getValue(Integer.parseInt(rootID));
    }

    public static void remove(String requestId) {
        WebStudioUtils.getExternalContext().getSessionMap().remove(Constants.SESSION_PARAM_PARAMETERS + requestId);
    }
}
