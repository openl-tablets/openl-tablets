package org.openl.rules.ruleservice.simple;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.util.StringTool;

/**
 * Simple implementation of IRulesFrontend interface.
 * 
 * @author Marat Kamalov
 * 
 */
public class RulesFrontendImpl implements RulesFrontend {
    private final Log log = LogFactory.getLog(RulesFrontendImpl.class);

    private Map<String, OpenLService> runningServices = new HashMap<String, OpenLService>();

    /** {@inheritDoc} */
    public OpenLService registerService(OpenLService service) {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }
        OpenLService replacedService = runningServices.put(service.getName(), service);
        if (replacedService != null && log.isWarnEnabled()) {
                log.warn(String.format(
                        "Service with name \"%s\" has been already registered. Replaced with new service bean.",
                        service.getName()));
        }
        return replacedService;
    }

    /** {@inheritDoc} */
    public OpenLService unregisterService(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        return runningServices.remove(serviceName);
    }

    /** {@inheritDoc} */
    public Collection<OpenLService> getServices() {
        return Collections.unmodifiableCollection(runningServices.values());
    }

    /** {@inheritDoc} */
    public OpenLService findServiceByName(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        return runningServices.get(serviceName);
    }

    /** {@inheritDoc} */
    public Object execute(String serviceName, String ruleName, Class<?>[] inputParamsTypes, Object[] params)
            throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }

        OpenLService service = runningServices.get(serviceName);
        if (service != null) {
            try {
                Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                        ruleName, inputParamsTypes);
                return serviceMethod.invoke(service.getServiceBean(), params);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Error during method \"%s\" calculation from the service \"%s\"", ruleName,
                            serviceName), e);
                }
                if (e.getCause() instanceof RuleServiceWrapperException) {
                    throw new MethodInvocationException(e.getMessage(), e.getCause());
                }
                throw new MethodInvocationException(e);
            }
        }else{
            throw new MethodInvocationException("Service not found!");
        }
    }

    /** {@inheritDoc} */
    public Object execute(String serviceName, String ruleName, Object... params) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Executing rule from service with name=\"%s\", ruleName=\"%s\"", serviceName,
                    ruleName));
        }

        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        return execute(serviceName, ruleName, paramTypes, params);
    }

    /** {@inheritDoc} */
    public Object getValue(String serviceName, String fieldName) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName argument can't be null");
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Getting value from service with name=\"%s\", fieldName=\"%s\"", serviceName,
                    fieldName));
        }

        Object result = null;

        OpenLService service = runningServices.get(serviceName);
        if (service != null) {
            try {
                Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                        StringTool.getGetterName(fieldName), new Class<?>[] {});
                result = serviceMethod.invoke(service.getServiceBean(), new Object[] {});
            } catch (Exception e) {
                if (e.getCause() instanceof RuleServiceWrapperException) {
                    throw new MethodInvocationException(e.getCause());
                }else{
                    if (log.isWarnEnabled()) {
                        log.warn(
                                String.format("Error reading field \"%s\" from the service \"%s\"", fieldName, serviceName),
                                e);
                    }                    
                }
            }
        }

        return result;
    }

}
