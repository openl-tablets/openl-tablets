package org.openl.rules.ruleservice.simple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.beanutils.MethodUtils;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.util.StringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation of IRulesFrontend interface.
 *
 * @author Marat Kamalov
 */
public class RulesFrontendImpl extends AbstractRulesFrontend {
    private final Logger log = LoggerFactory.getLogger(RulesFrontendImpl.class);

    private Map<String, OpenLService> runningServices = new ConcurrentHashMap<String, OpenLService>();

    /**
     * {@inheritDoc}
     */
    public void registerService(OpenLService service) {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }
        OpenLService replacedService = runningServices.put(service.getName(), service);
        if (replacedService != null) {
            log.warn("Service with name \"{}\" has been already registered. Replaced with new service bean.",
                service.getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterService(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        runningServices.remove(serviceName);
    }

    // for internal usage
    Collection<OpenLService> getServices() {
        return new ArrayList<OpenLService>(runningServices.values());
    }

    public java.util.Collection<String> getServiceNames() {
        return new ArrayList<String>(runningServices.keySet());
    };

    // for internal usage
    @Deprecated
    public OpenLService findServiceByName(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        return runningServices.get(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    public Object execute(String serviceName,
            String ruleName,
            Class<?>[] inputParamsTypes,
            Object[] params) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }

        OpenLService service = runningServices.get(serviceName);
        if (service != null) {
            Method serviceMethod = null;
            serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                ruleName,
                inputParamsTypes);
            if (serviceMethod == null) {
                StringBuilder sb = new StringBuilder();
                boolean f = true;
                for (Class<?> param : inputParamsTypes) {
                    if (!f) {
                        sb.append(",");
                    } else {
                        f = false;
                    }
                    sb.append(param.getCanonicalName());
                }

                throw new MethodInvocationException("Method with name '" + ruleName + "(" + sb
                    .toString() + ")' not found in service '" + serviceName + "'!");
            }
            try {
                return serviceMethod.invoke(service.getServiceBean(), params);
            } catch (IllegalAccessException e) {
                throw new InternalError(e.toString());
            } catch (InvocationTargetException e) {
                Throwable t = e.getCause();
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new MethodInvocationException(t.toString(), t);
                }
            } 
        } else {
            throw new MethodInvocationException("Service not found!");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object execute(String serviceName, String ruleName, Object... params) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }

        log.debug("Executing rule from service with name=\"{}\", ruleName=\"{}\"", serviceName, ruleName);

        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        return execute(serviceName, ruleName, paramTypes, params);
    }

    /**
     * {@inheritDoc}
     */
    public Object getValue(String serviceName, String fieldName) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName argument can't be null");
        }

        log.debug("Getting value from service with name=\"{}\", fieldName=\"{}\"", serviceName, fieldName);

        Object result = null;

        OpenLService service = runningServices.get(serviceName);
        if (service != null) {
            try {
                Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                    StringTool.getGetterName(fieldName),
                    new Class<?>[] {});
                result = serviceMethod.invoke(service.getServiceBean(), new Object[] {});
            } catch (Exception e) {
                if (e.getCause() instanceof RuleServiceWrapperException) {
                    throw new MethodInvocationException(e.getCause());
                } else {
                    log.warn("Error reading field \"{}\" from the service \"{}\"", fieldName, serviceName, e);
                }
            }
        }

        return result;
    }

}
