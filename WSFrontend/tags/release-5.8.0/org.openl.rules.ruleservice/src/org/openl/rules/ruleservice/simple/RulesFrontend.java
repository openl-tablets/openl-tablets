package org.openl.rules.ruleservice.simple;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.util.StringTool;

/**
 * Simple implementation of IRulesFrontend interface
 * 
 * @author MKamalov
 * 
 */
public class RulesFrontend implements IRulesFrontend {
    private Log log = LogFactory.getLog(RulesFrontend.class);

    private Map<String, OpenLService> runningServices = new HashMap<String, OpenLService>();

    /** {@inheritDoc} */
    public void registerService(OpenLService service) {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }
        runningServices.put(service.getName(), service);
    }

    /** {@inheritDoc} */
    public void unregisterService(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        runningServices.remove(serviceName);
    }

    /** {@inheritDoc} */
    public List<OpenLService> getServices() {
        return Collections.unmodifiableList(new ArrayList<OpenLService>(runningServices.values()));
    }

    /** {@inheritDoc} */
    public Object execute(String serviceName, String ruleName, Class<?>[] inputParamsTypes, Object[] params) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }
        Object result = null;

        OpenLService service = runningServices.get(serviceName);
        if (service != null) {
            try {
                Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                        ruleName, inputParamsTypes);
                result = serviceMethod.invoke(service.getServiceBean(), params);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Error during method \"%s\" calculation from the service \"%s\"", ruleName,
                            serviceName), e);
                }
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    public Object execute(String serviceName, String ruleName, Object... params) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug("Executing rule from service with name=" + serviceName + ", ruleName=" + ruleName);
        }

        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        return execute(serviceName, ruleName, paramTypes, params);
    }

    /** {@inheritDoc} */
    public Object getValues(String serviceName, String fieldName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug("Getting value from service with name=" + serviceName + ", fieldName= " + fieldName);
        }

        Object result = null;

        OpenLService service = runningServices.get(serviceName);
        if (service != null) {
            try {
                Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                        StringTool.getGetterName(fieldName), new Class<?>[] {});
                result = serviceMethod.invoke(service.getServiceBean(), new Object[] {});
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn(
                            String.format("Error reading field \"%s\" from the service \"%s\"", fieldName, serviceName),
                            e);
                }
            }
        }

        return result;
    }

}
