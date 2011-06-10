package org.openl.ruleservice.simple;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.ruleservice.OpenLService;
import org.openl.util.StringTool;

public class RulesFrontendImpl implements RulesFrontend {
    private static final Log LOG = LogFactory.getLog(RulesFrontendImpl.class);

    private Map<String, OpenLService> runningServices = new HashMap<String, OpenLService>();

    public void registerService(OpenLService service) {
        runningServices.put(service.getName(), service);
    }

    public void unregisterService(String serviceName) {
        runningServices.remove(serviceName);
    }

    /*private void checkMethodDeclarationInServiceClass(String serviceName, String methodName, Class<?>[] inputParamsTypes) {
        OpenLService service = runningServices.get(serviceName);
        Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceClass(), methodName,
                inputParamsTypes);
        if (serviceMethod == null) {
            throw new RuntimeException("There are no such method declared in service class.");
        }
    }*/

    public Object execute(String serviceName, String ruleName, Class<?>[] inputParamsTypes, Object[] params) {
        Object result = null;

        OpenLService service = runningServices.get(serviceName);
        if (service != null) {
            try {
                Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                        ruleName, inputParamsTypes);
                result = serviceMethod.invoke(service.getServiceBean(), params);
            } catch (Exception e) {
                LOG.warn(String.format("Error during method \"%s\" calculation from the service \"%s\"", ruleName,
                        serviceName), e);
            }
        }

        return result;
    }

    public Object execute(String serviceName, String ruleName, Object... params) {
        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        return execute(serviceName, ruleName, paramTypes, params);
    }

    public Object getValues(String serviceName, String fieldName) {
        Object result = null;

        OpenLService service = runningServices.get(serviceName);
        if (service != null) {
            try {
                Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                        StringTool.getGetterName(fieldName), new Class<?>[] {});
                result = serviceMethod.invoke(service.getServiceBean(), new Object[] {});
            } catch (Exception e) {
                LOG.warn(String.format("Error reading field \"%s\" from the service \"%s\"", fieldName, serviceName), e);
            }
        }

        return result;
    }

}
