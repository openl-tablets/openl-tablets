package org.openl.rules.ruleservice.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdviceListener;

class ServiceInvocationAdviceTest {

    @Test
    void refusesAServiceWithoutItsModule() {
        var serviceTarget = new Object();
        Map<Method, Method> methodMap = Map.of();
        var classLoader = getClass().getClassLoader();
        List<ServiceInvocationAdviceListener> listeners = List.of();
        var applicationContext = mock(ApplicationContext.class);
        Optional<RulesDeploy> rulesDeploy = Optional.empty();
        Optional<ProjectDescriptor> projectDescriptor = Optional.empty();

        var error = assertThrows(NullPointerException.class,
                () -> new ServiceInvocationAdvice(null,
                        serviceTarget,
                        methodMap,
                        classLoader,
                        listeners,
                        applicationContext,
                        rulesDeploy,
                        projectDescriptor));

        assertEquals("openClass", error.getMessage());
    }
}
