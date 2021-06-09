package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.objectweb.asm.Type;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "production-repository.uri=test-resources/DynamicInterfacePublishingTest",
        "production-repository.factory = repo-file"})
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class DynamicInterfacePublishingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void publishWithDynamicInterface() throws RuleServiceInstantiationException {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        OpenLService service = serviceManager.getServiceByDeploy("DynamicInterfacePublishingTest/project1");
        assertNotNull(service);
        assertNotNull(service.getServiceClass());
        String[] methods = {
                "method2(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/generated/beans/MyType;)Lorg/openl/generated/beans/MyType;",
                "method2(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/ruleservice/dynamicinterface/test/MyClass;)Lorg/openl/ruleservice/dynamicinterface/test/MyClass;",
                "method1(Lorg/openl/rules/context/IRulesRuntimeContext;Ljava/lang/Object;)Ljava/lang/String;",
                "method3(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/ruleservice/dynamicinterface/test/MyClass;)Lorg/openl/ruleservice/dynamicinterface/test/MyClass;",
                "baseHello(Lorg/openl/rules/context/IRulesRuntimeContext;I)Ljava/lang/String;",
                "baseHello2(Lorg/openl/rules/context/IRulesRuntimeContext;I)Ljava/lang/String;" };
        Set<String> methodNames = new HashSet<>();
        Collections.addAll(methodNames, methods);
        int count = 0;
        for (Method method : service.getServiceClass().getMethods()) {
            if (methodNames.contains(method.getName() + Type.getMethodDescriptor(method))) {
                count++;
            }
        }
        Assert.assertEquals(methods.length, count);
    }

    @Test
    public void publishWithDynamicInterfaceMethodFilter() throws RuleServiceInstantiationException {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        OpenLService service = serviceManager.getServiceByDeploy("DynamicInterfacePublishingTest/project2");
        assertNotNull(service);
        assertNotNull(service.getServiceClass());

        String[] methods = {
                "method2(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/generated/beans/MyType;)Lorg/openl/generated/beans/MyType;",
                "method2(Lorg/openl/rules/context/IRulesRuntimeContext;Lorg/openl/ruleservice/dynamicinterface/test/MyClass;)Lorg/openl/ruleservice/dynamicinterface/test/MyClass;" };
        Set<String> methodNames = new HashSet<>();
        Collections.addAll(methodNames, methods);
        int count = 0;
        for (Method method : service.getServiceClass().getMethods()) {
            if (methodNames.contains(method.getName() + Type.getMethodDescriptor(method))) {
                count++;
            }
        }
        Assert.assertEquals(methods.length, count);
    }

    @Test
    public void publishWithDynamicInterfaceMethodInterceptingTest() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        OpenLService service = serviceManager.getServiceByDeploy("DynamicInterfacePublishingTest/project3");
        assertNotNull(service);
        assertNotNull(service.getServiceClass());

        Annotation webServiceAnn = service.getServiceClass().getAnnotation(ClassLevelAnnotation.class);
        assertNotNull(webServiceAnn);

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Class<?> myClassClass = service.getServiceClass()
            .getClassLoader()
            .loadClass("org.openl.ruleservice.dynamicinterface.test.MyClass");
        Object myClassInstance = myClassClass.newInstance();
        Method setNameMethod = myClassClass.getMethod("setName", String.class);
        final String someValue = "someValue";
        setNameMethod.invoke(myClassInstance, someValue);
        Object result = frontend
            .execute("dynamic-interface-test3", "method2", context, myClassInstance);
        Assert.assertTrue(myClassClass.isInstance(result));
        Method getNameMethod = myClassClass.getMethod("getName");
        Object name = getNameMethod.invoke(result);
        Assert.assertEquals(someValue, name);
        Class<?> myTypeClass = service.getServiceClass().getClassLoader().loadClass("org.openl.generated.beans.MyType");
        Object myTypeInstance = myTypeClass.newInstance();
        result = frontend.execute("dynamic-interface-test3", "method2", context, myTypeInstance);
        Assert.assertNull(result);
        frontend.execute("dynamic-interface-test3", "method3", context, myClassInstance);
        Object value = getNameMethod.invoke(myClassInstance);
        Assert.assertEquals("beforeAdviceWasInvoked", value);

        result = frontend.execute("dynamic-interface-test3", "helloWorld");
        Assert.assertEquals("Hello world ServiceExtraMethodHandler!", result);
    }
}
