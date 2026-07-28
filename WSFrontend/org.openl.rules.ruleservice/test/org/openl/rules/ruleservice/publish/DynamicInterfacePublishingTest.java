package org.openl.rules.ruleservice.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;

@TestPropertySource(properties = {"production-repository.uri=test-resources/DynamicInterfacePublishingTest",
        "production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
class DynamicInterfacePublishingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void publishWithDynamicInterface() throws RuleServiceInstantiationException {
        assertNotNull(applicationContext);
        var serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        var frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        var service = serviceManager.getServiceByDeploy("DynamicInterfacePublishingTest/project1");
        assertNotNull(service);
        assertNotNull(service.getServiceClass());
        String[] methods = {
                "method2(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/generated/beans/MyType;)Lorg/openl/generated/beans/MyType;",
                "method2(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/ruleservice/dynamicinterface/test/MyClass;)Lorg/openl/ruleservice/dynamicinterface/test/MyClass;",
                "method1(Lorg/openl/rules/context/IRulesRuntimeContext;Ljava/lang/Object;)Ljava/lang/String;",
                "method3(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/ruleservice/dynamicinterface/test/MyClass;)Lorg/openl/ruleservice/dynamicinterface/test/MyClass;",
                "baseHello(Lorg/openl/rules/context/IRulesRuntimeContext;I)Ljava/lang/String;",
                "baseHello2(Lorg/openl/rules/context/IRulesRuntimeContext;I)Ljava/lang/String;"};
        var methodNames = new HashSet<String>();
        Collections.addAll(methodNames, methods);
        var count = 0;
        for (Method method : service.getServiceClass().getMethods()) {
            if (methodNames.contains(method.getName() + Type.getMethodDescriptor(method))) {
                count++;
            }
        }
        assertEquals(methods.length, count);
    }

    @Test
    void publishWithDynamicInterfaceMethodFilter() throws RuleServiceInstantiationException {
        assertNotNull(applicationContext);
        var serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        var frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        var service = serviceManager.getServiceByDeploy("DynamicInterfacePublishingTest/project2");
        assertNotNull(service);
        assertNotNull(service.getServiceClass());

        String[] methods = {
                "method2(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/generated/beans/MyType;)Lorg/openl/generated/beans/MyType;",
                "method2(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/ruleservice/dynamicinterface/test/MyClass;)Lorg/openl/ruleservice/dynamicinterface/test/MyClass;"};
        var methodNames = new HashSet<String>();
        Collections.addAll(methodNames, methods);
        var count = 0;
        for (Method method : service.getServiceClass().getMethods()) {
            if (methodNames.contains(method.getName() + Type.getMethodDescriptor(method))) {
                count++;
            }
        }
        assertEquals(methods.length, count);
    }

    @Test
    void publishWithDynamicInterfaceMethodInterceptingTest() throws Exception {
        assertNotNull(applicationContext);
        var serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        var frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        var service = serviceManager.getServiceByDeploy("DynamicInterfacePublishingTest/project3");
        assertNotNull(service);
        assertNotNull(service.getServiceClass());

        var webServiceAnn = service.getServiceClass().getAnnotation(ClassLevelAnnotation.class);
        assertNotNull(webServiceAnn);

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        var myClassClass = service.getServiceClass()
                .getClassLoader()
                .loadClass("org.openl.ruleservice.dynamicinterface.test.MyClass");
        var myClassInstance = myClassClass.getDeclaredConstructor().newInstance();
        var setNameMethod = myClassClass.getMethod("setName", String.class);
        final var someValue = "someValue";
        setNameMethod.invoke(myClassInstance, someValue);
        var result = frontend
                .execute("dynamic-interface-test3", "method2", context, myClassInstance);
        assertTrue(myClassClass.isInstance(result));
        var getNameMethod = myClassClass.getMethod("getName");
        var name = getNameMethod.invoke(result);
        assertEquals(someValue, name);
        var myTypeClass = service.getServiceClass().getClassLoader().loadClass("org.openl.generated.beans.MyType");
        var myTypeInstance = myTypeClass.getDeclaredConstructor().newInstance();
        result = frontend.execute("dynamic-interface-test3", "method2", context, myTypeInstance);
        assertNull(result);
        frontend.execute("dynamic-interface-test3", "method3", context, myClassInstance);
        var value = getNameMethod.invoke(myClassInstance);
        assertEquals("beforeAdviceWasInvoked", value);

        result = frontend.execute("dynamic-interface-test3", "helloWorld");
        assertEquals("Hello world ServiceExtraMethodHandler!", result);
    }
}
