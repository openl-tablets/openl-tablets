package org.openl.ruleservice.dynamicinterface.test;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.AnyType;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;

@org.openl.rules.ruleservice.publish.ClassLevelAnnotation
public interface MyTemplateClass {
    @ServiceCallAfterInterceptor(value = { MyAfterAdvice.class })
    MyClass method2(IRulesRuntimeContext context, @AnyType(".*MyType") Object obj);

    @ServiceCallBeforeInterceptor(value = { MyBeforeAdvice.class })
    MyClass method3(@AnyType Object obj, @AnyType Object obj1);

    @ServiceExtraMethod(ServiceExtraMethodHandler.class)
    String helloWorld();
}
