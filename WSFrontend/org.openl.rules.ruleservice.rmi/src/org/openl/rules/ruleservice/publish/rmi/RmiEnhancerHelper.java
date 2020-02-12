package org.openl.rules.ruleservice.publish.rmi;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.rmi.DefaultRmiHandler;
import org.openl.runtime.OpenLJavaAssistProxy;

/**
 * Utility class for generate RMI annotations for service interface.
 *
 * @author Marat Kamalov
 *
 */
public class RmiEnhancerHelper {

    private RmiEnhancerHelper() {
    }

    private static ClassLoader getClassLoader(OpenLService service) throws RuleServiceInstantiationException {
        ClassLoader classLoader = null;
        if (service != null) {
            classLoader = service.getClassLoader();
        }
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    public static DefaultRmiHandler decorateBeanWithDynamicRmiHandler(Object targetBean,
            OpenLService service) throws Exception {
        Class<?> serviceClass = service.getServiceClass();
        Map<String, List<Method>> methodMap = new HashMap<>();
        for (Method method : serviceClass.getMethods()) {
            List<Method> methods = null;
            if (methodMap.containsKey(method.getName())) {
                methods = methodMap.get(method.getName());
            } else {
                methods = new ArrayList<>();
                methodMap.put(method.getName(), methods);
            }
            methods.add(method);
        }
        return new DefaultRmiMethodHandler(targetBean, methodMap);
    }

    public static Remote decorateBeanWithStaticRmiHandler(Object targetBean, OpenLService service) throws Exception {
        Class<?> serviceClass = service.getServiceClass();
        Map<Method, Method> methodMap = new HashMap<>();

        for (Method m : service.getRmiServiceClass().getMethods()) {
            boolean found = false;
            for (Method method : serviceClass.getMethods()) {
                if (m.getName()
                    .equals(method.getName()) && m.getParameterTypes().length == method.getParameterTypes().length) {
                    boolean f = true;
                    for (int i = 0; i < method.getParameterTypes().length; i++) {
                        if (!m.getParameterTypes()[i].equals(method.getParameterTypes()[i])) {
                            f = false;
                            break;
                        }
                    }
                    if (f) {
                        methodMap.put(m, method);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                throw new RuleServiceRuntimeException(
                    "Failed to create a proxy for the service. RMI interface contains a method that is not found in the service interface.");
            }
        }

        return (Remote) OpenLJavaAssistProxy.create(getClassLoader(service),
            new StaticRmiMethodHandler(targetBean, methodMap),
            new Class<?>[] { service.getRmiServiceClass() });
    }
}
