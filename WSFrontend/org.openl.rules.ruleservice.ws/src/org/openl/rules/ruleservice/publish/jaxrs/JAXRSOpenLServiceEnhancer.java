package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import io.swagger.v3.oas.models.OpenAPI;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.runtime.ASMProxyFactory;

/**
 * Utility class for generate JAXRS annotations for service interface.
 *
 * @author Marat Kamalov
 */
public final class JAXRSOpenLServiceEnhancer {

    private static ProjectDescriptor extractProjectDescriptor(OpenLService service) {
        for (Module module : service.getModules()) {
            if (module.getProject() != null) {
                return module.getProject();
            }
        }
        return null;
    }

    public Object decorateServiceBean(OpenLService service) throws Exception {
        Class<?> serviceClass = service.getServiceClass();
        Objects.requireNonNull(serviceClass, "Service class cannot be null");
        ClassLoader classLoader = service.getClassLoader();

        ProjectDescriptor projectDescriptor = extractProjectDescriptor(service);
        OpenAPI openApi = null;
        if (projectDescriptor != null) {
            openApi = OpenAPIFileUtils.loadOpenAPI(projectDescriptor, service.getCompiledOpenClass());
        }
        Class<?> enhancedServiceClass = JAXRSOpenLServiceEnhancerHelper.enhanceInterface(
                serviceClass,
                service.getServiceBean(),
                classLoader,
                openApi,
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
