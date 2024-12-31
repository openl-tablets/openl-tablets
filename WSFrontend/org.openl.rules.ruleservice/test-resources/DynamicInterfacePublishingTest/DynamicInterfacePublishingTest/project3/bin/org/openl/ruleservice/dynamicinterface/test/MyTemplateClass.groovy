package org.openl.ruleservice.dynamicinterface.test;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;

@org.openl.rules.ruleservice.publish.ClassLevelAnnotation
public interface MyTemplateClass {
    @ServiceCallAfterInterceptor(MyAfterAdvice.class)
    MyClass method2(IRulesRuntimeContext context, @RulesType("MyType") Object obj);

    @ServiceCallBeforeInterceptor(MyBeforeAdvice.class)
    MyClass method3(IRulesRuntimeContext context, MyClass obj1);

    @ServiceExtraMethod(ServiceExtraMethodHandler.class)
    String helloWorld();
}
