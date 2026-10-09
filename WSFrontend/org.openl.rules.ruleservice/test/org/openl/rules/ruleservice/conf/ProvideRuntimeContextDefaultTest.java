package org.openl.rules.ruleservice.conf;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.management.ServiceManager;

/**
 * The deployment-wide {@code ruleservice.isProvideRuntimeContext} applies to a project without the
 * {@code isProvideRuntimeContext} tag, and the tag of {@code rules-deploy.xml} wins over it.
 */
@TestPropertySource(properties = {"production-repository.uri=test-resources/ProvideRuntimeContextDefaultTest",
        "ruleservice.isProvideRuntimeContext=true",
        "production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
class ProvideRuntimeContextDefaultTest {

    @Autowired
    private ServiceManager serviceManager;

    @Test
    void projectWithoutTheTagTakesTheDeploymentWideDefault() throws Exception {
        var methods = ruleMethods("ProvideRuntimeContextDefaultTest/inherited");
        assertTrue(Arrays.stream(methods).allMatch(ProvideRuntimeContextDefaultTest::takesRuntimeContext));
    }

    @Test
    void tagOfTheProjectWinsOverTheDeploymentWideDefault() throws Exception {
        var methods = ruleMethods("ProvideRuntimeContextDefaultTest/pinned");
        assertTrue(Arrays.stream(methods).noneMatch(ProvideRuntimeContextDefaultTest::takesRuntimeContext));
    }

    private Method[] ruleMethods(String deployPath) throws Exception {
        var service = serviceManager.getServiceByDeploy(deployPath);
        assertNotNull(service, "Service '" + deployPath + "' is not deployed");
        var methods = service.getServiceClass().getMethods();
        assertNotEquals(0, methods.length);
        return methods;
    }

    private static boolean takesRuntimeContext(Method method) {
        return method.getParameterCount() > 0 && IRulesRuntimeContext.class.equals(method.getParameterTypes()[0]);
    }
}
