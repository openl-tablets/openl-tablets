package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.runtime.ASMProxyFactory;

/**
 * Utility class for generate JAXRS annotations for service interface.
 *
 * @author Marat Kamalov
 */
public final class JAXRSOpenLServiceEnhancer {

    public Object decorateServiceBean(OpenLService service) throws Exception {
        Class<?> serviceClass = service.getServiceClass();
        Objects.requireNonNull(serviceClass, "Service class cannot be null");
        ClassLoader classLoader = service.getClassLoader();
        Class<?> enhancedServiceClass = JAXRSOpenLServiceEnhancerHelper.enhanceInterface(serviceClass,
                service.getServiceBean(),
                classLoader,
                service.isProvideRuntimeContext(),
                service.isProvideVariations()
        );
        if (enhancedServiceClass.getPackage() == null) {
            throw new IllegalStateException("Package cannot be null");
        }

        Map<Method, Method> methodMap = JAXRSOpenLServiceEnhancerHelper.buildMethodMap(serviceClass,
                enhancedServiceClass);
        return ASMProxyFactory.newProxyInstance(classLoader,
                new JAXRSMethodHandler(service.getServiceBean(), methodMap),
                enhancedServiceClass);
    }
}
